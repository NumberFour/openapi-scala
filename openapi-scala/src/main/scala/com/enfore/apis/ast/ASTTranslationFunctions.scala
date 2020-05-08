package com.enfore.apis.ast

import cats.data.NonEmptyList
import cats.implicits._
import com.enfore.apis.ast.SwaggerAST.RequestType.{PATCH, POST, PUT}
import com.enfore.apis.ast.SwaggerAST._
import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr._

import scala.collection.immutable
import scala.util.matching.Regex

object ASTTranslationFunctions {

  val camelCaseExpr: Regex = "(([^_A-Z])([A-Z])+)".r

  final case class PackageName(name: String)

  // Helper functions

  private def mapValues[K, V, X](in: Map[K, V])(f: V => X): Map[K, X] = in.map {
    case (key, value) => key -> f(value)
  }

  // Functions for translating routes

  private def extractMediaTypeObject(
      schemaObject: MediaTypeObject
  )(implicit packageName: PackageName): Option[TypeRepr] =
    schemaObject.schema.flatMap { loadSingleProperty }

  private def extractRefOfMediaTypeObject(schemaObject: MediaTypeObject): Option[Ref] =
    schemaObject.schema
      .flatMap {
        case _: SchemaObject         => None
        case ReferenceObject(ref, _) => Some(Ref(ref, ref.split("/").last, None))
      }

  private def getBodyEncodings(media: RequestBodyObject)(implicit packageName: PackageName): List[TypeRepr] =
    media.content.values.toList.map { extractMediaTypeObject }.sequence.toList.flatten

  private def getEncodedMap(media: ResponseObject): Map[String, Ref] =
    mapValues(
      mapValues(media.content.getOrElse(Map.empty))(extractRefOfMediaTypeObject)
        .filter(_._2.isDefined)
    )(_.get)

  private def getNameContentEncoding(
      mediaMap: ResponsesObject,
      acceptMedia: Int => Boolean
  ): Option[(Int, Map[String, Ref])] = mediaMap.find(x => acceptMedia(x._1)).map(x => (x._1, getEncodedMap(x._2)))

  private def translateReqContentType(in: RequestType): ReqWithContentType = in match {
    case POST  => ReqWithContentType.POST
    case PUT   => ReqWithContentType.PUT
    case PATCH => ReqWithContentType.PATCH
    case other => throw new RuntimeException(s"Invalid RequestType $other for Request with Content")
  }

  private def extractPathParameters(parameters: List[SwaggerAST.ParameterObject]): List[PathParameter] =
    parameters.filter(_.in == ParameterLocation.path).map(x => PathParameter(x.name))

  private def extractQueryParameters(
      parameters: List[SwaggerAST.ParameterObject]
  )(implicit packageName: PackageName): Map[String, TypeRepr] =
    parameters
      .filter(_.in == ParameterLocation.query)
      .map(
        (y: ParameterObject) =>
          y.name -> {
            val isRequired: Boolean = y.required.getOrElse(false)
            y.schema.get match {
              case a: SchemaObject =>
                val refinements: List[RefinedTags] = List(
                  a.minimum.map(Minimum),
                  a.maximum.map(Maximum),
                  a.minLength.map(MinLength),
                  a.maxLength.map(MaxLength)
                ).flatten
                val value: Primitive =
                  buildPrimitiveFromSchemaObjectType(NonEmptyList.fromList(refinements), a.items)(packageName)(
                    a.`type`.get
                  ).get
                if (!isRequired && a.default.isEmpty) {
                  PrimitiveOption(dataType = value)
                } else {
                  value
                }
              case referenceObject: ReferenceObject =>
                val value = loadSingleProperty(referenceObject).get
                if (!isRequired && referenceObject.default.isEmpty) {
                  PrimitiveOption(dataType = value)
                } else {
                  value
                }
            }
          }
      )
      .toMap

  private def findRefInComponents(
      components: Map[String, SchemaObject],
      typeName: String,
      context: String = ""
  ): SchemaObject =
    components
      .collectFirst { case (key: String, schemaObject: SchemaObject) if key == typeName => schemaObject }
      .getOrElse(
        throw new Exception(s"The component $typeName is not found in components ($context)")
      )

  private def hasReadOnlyType(context: String, components: Map[String, SchemaObject]): TypeRepr => Boolean = {
    case _: Primitive => false
    case body: TypeRepr =>
      schemaObjectHasReadOnlyComponent(components)(findRefInComponents(components, body.typeName, context))
  }

  private[ast] def routeDefFromSwaggerAST(path: String)(route: OperationObject, requestType: RequestType)(
      components: Map[String, SchemaObject]
  )(implicit packageName: PackageName): RouteDefinition = {
    val parameters: List[ParameterObject]   = route.parameters.getOrElse(List.empty[ParameterObject])
    val pathParameters: List[PathParameter] = extractPathParameters(parameters)
    pathParameters foreach { param: PathParameter =>
      assert(path.contains(param.name), s"The path $path does not contain parameter ${param.name}")
    }
    val queryParams: Map[String, TypeRepr] = extractQueryParameters(parameters)
    val possibleBodies: List[TypeRepr]     = route.requestBody.toList.flatMap(getBodyEncodings)
    val possibleResponse: Option[(Int, Map[String, Ref])] =
      getNameContentEncoding(route.responses, httpStatus => httpStatus >= 200 && httpStatus < 300)
    assert(possibleResponse.nonEmpty, "There has to be one successful (>=200 and <300) return code")
    requestType match {
      case RequestType.POST | RequestType.PUT | RequestType.PATCH =>
        RequestWithPayload(
          path = path,
          summary = route.summary,
          description = route.description,
          operationId = route.operationId,
          `type` = translateReqContentType(requestType),
          pathParams = pathParameters,
          queries = queryParams,
          request = possibleBodies.headOption,
          response = possibleResponse.map(_._2),
          hasReadOnlyType = possibleBodies.headOption.map(
            hasReadOnlyType(s"referenced in $path $requestType", components)
          ),
          successStatusCode = possibleResponse.get._1
        )
      case RequestType.GET =>
        GetRequest(
          path = path,
          summary = route.summary,
          description = route.description,
          operationId = route.operationId,
          pathParams = pathParameters,
          queries = queryParams,
          response = possibleResponse.map(_._2),
          successStatusCode = possibleResponse.get._1
        )
      case RequestType.DELETE =>
        DeleteRequest(
          path = path,
          summary = route.summary,
          description = route.description,
          operationId = route.operationId,
          pathParams = pathParameters,
          response = possibleResponse.map(_._2),
          successStatusCode = possibleResponse.get._1
        )
    }
  }

  private def readToRoutes(paths: Map[String, Map[String, OperationObject]])(
      components: Map[String, SchemaObject]
  )(implicit packageName: PackageName): List[RouteDefinition] =
    paths.flatMap {
      case (path: String, routes: Map[String, OperationObject]) =>
        val patch: Option[RouteDefinition] =
          routes.get("patch").map(routeDefFromSwaggerAST(path)(_, RequestType.PATCH)(components))
        val put: Option[RouteDefinition] =
          routes.get("put").map(routeDefFromSwaggerAST(path)(_, RequestType.PUT)(components))
        val post: Option[RouteDefinition] =
          routes.get("post").map(routeDefFromSwaggerAST(path)(_, RequestType.POST)(components))
        val get: Option[RouteDefinition] =
          routes.get("get").map(routeDefFromSwaggerAST(path)(_, RequestType.GET)(components))
        val delete: Option[RouteDefinition] =
          routes.get("delete").map(routeDefFromSwaggerAST(path)(_, RequestType.DELETE)(components))
        List(patch, put, post, get, delete).flatten
    }.toList

  //  Functions for translating components

  private val illegalFilePathSymbols = List("/")

  private def cleanFilename(name: String): String =
    illegalFilePathSymbols.foldLeft(name) {
      case (filename: String, symbol: String) => filename.replaceAll(symbol, "_")
    }

  private def buildPrimitiveFromSchemaObjectTypeForComponents(
      items: Option[SchemaOrReferenceObject],
      refinements: Option[NonEmptyList[RefinedTags]]
  )(
      implicit packageName: PackageName
  ): SchemaObjectType => Option[TypeRepr] = {
    case schemaObject @ SchemaObjectType.`object` =>
      throw new AssertionError(s"Inline object definition for $schemaObject is not possible")
    case SchemaObjectType.array =>
      assert(items.isDefined, "A parameter type for an array is not defined")
      loadSingleProperty(items.get).map(PrimitiveArray(_, refinements))
    case SchemaObjectType.`dict` =>
      assert(items.isDefined, "A parameter type for a dictionary is not defined")
      loadSingleProperty(items.get).map(PrimitiveDict(_, refinements))
    case x => buildPrimitiveFromSchemaObjectType(refinements)(packageName)(x)
  }

  private def buildPrimitiveFromSchemaObjectType(
      refinements: Option[NonEmptyList[TypeRepr.RefinedTags]],
      items: Option[SchemaOrReferenceObject] = None
  )(implicit packageName: PackageName): SchemaObjectType => Option[Primitive] = {
    case SchemaObjectType.string    => Some(PrimitiveString(refinements, None))
    case SchemaObjectType.boolean   => Some(PrimitiveBoolean(refinements, None))
    case SchemaObjectType.number    => Some(PrimitiveNumber(refinements, None))
    case SchemaObjectType.`integer` => Some(PrimitiveInt(refinements, None))
    case SchemaObjectType.`array` =>
      val loadedType = items.flatMap {
        case so: SchemaObject =>
          so.`type`.flatMap(sot => buildPrimitiveFromSchemaObjectType(refinements)(packageName)(sot))
        case ro: ReferenceObject =>
          loadSingleProperty(ro)
      } getOrElse (throw new Exception("Unexpected behavior in buildPrimitiveFromSchemaObjectType"))
      Some(PrimitiveArray(dataType = loadedType, refinements = None))
    case x =>
      throw new AssertionError(
        s"The case $x does not seem to be supported by the generator: buildPrimitiveFromSchemaObjectType"
      )
  }

  private def loadSingleProperty(
      property: SchemaOrReferenceObject
  )(implicit packageName: PackageName): Option[TypeRepr] =
    property match {
      case so: SchemaObject =>
        so.additionalProperties.fold {
          val refinements: Option[NonEmptyList[CollectionRefinements]] =
            List(so.minLength.map(MinLength), so.maxLength.map(MaxLength))
              .filter(_.isDefined)
              .sequence
              .flatMap(NonEmptyList.fromList)
          so.`type`
            .flatMap(buildPrimitiveFromSchemaObjectTypeForComponents(so.items, refinements))
        }(loadSingleProperty(_).map(PrimitiveDict(_, None)))
      case ReferenceObject(ref, _) =>
        val name: String = ref.split("/").last
        val path: String = ref.split("/").dropRight(1).mkString(".")
        (Ref(path.replace("#.components.schemas", packageName.name), name, None): TypeRepr).some
    }

  private def makeSymbolFromTypeRepr(name: String, repr: TypeRepr): Symbol = repr match {
    case p: Primitive => PrimitiveSymbol(name, p)
    case n: NewType   => NewTypeSymbol(name, n)
    case r: Ref       => RefSymbol(name, r)
  }

  private def loadObjectProperties(
      typeName: String,
      properties: Map[String, SchemaOrReferenceObject],
      required: List[String],
      summary: Option[String],
      description: Option[String],
      allSchemas: Map[String, SchemaObject]
  )(
      implicit packageName: PackageName
  ): Option[NewType] = {
    val mapped: immutable.Iterable[Option[Symbol]] = properties map {
      case (name: String, repr: SchemaOrReferenceObject) =>
        if (camelCaseExpr.findAllIn(name).nonEmpty)
          println(s"[WARN] You are using camel case name in $name. JSON serialiser will only respect snake cases.")
        val loaded: Option[TypeRepr] = getTypeRepr(required, name, repr, allSchemas)
        assert(loaded.isDefined, s"$name in $typeName could not be parsed.")
        loaded map (makeSymbolFromTypeRepr(name, _))
    }
    mapped.toList.sequence.map(PrimitiveProduct(packageName.name, typeName, _, summary, description))
  }

  private def getTypeRepr(
      required: List[String],
      name: String,
      repr: SchemaOrReferenceObject,
      allProps: Map[String, SchemaObject]
  )(
      implicit packageName: PackageName
  ): Option[TypeRepr] =
    repr match {
      case o: SchemaObject if o.oneOf.nonEmpty =>
        throw new AssertionError("Discriminated Unions (OpenAPI: oneOf) are only supported as top-level types for now.")
      case r: ReferenceObject =>
        val typeRepr    = loadSingleProperty(repr)
        val referenceTo = r.$ref.split("/").last.stripSuffix("Request")
        allProps
          .getOrElse(
            referenceTo,
            throw new Exception(
              s"You're referencing ${r.$ref} which does not exist. Available components are ${allProps.keys.mkString(", ")}"
            )
          )
          .default
          .fold(if (required contains name) typeRepr else typeRepr.map(PrimitiveOption))(
            dv => typeRepr.map(_.packDefault(dv))
          )
      case _ =>
        val typeRepr: Option[TypeRepr] = loadSingleProperty(repr)
        repr.default.fold(if (required contains name) typeRepr else typeRepr.map(PrimitiveOption))(
          dv => typeRepr.map(_.packDefault(dv))
        )
    }

  private def loadEnum(
      typeName: String,
      values: List[String],
      summary: Option[String],
      description: Option[String]
  )(
      implicit packageName: PackageName
  ): NewType =
    PrimitiveEnum(packageName.name, typeName, values.toSet, summary, description)

  private def handleSchemaObjectProductType(
      name: String,
      schemaObject: SchemaObject,
      allSchemas: Map[String, SchemaObject]
  )(
      implicit packageName: PackageName
  ): Option[Symbol] = {
    val required = schemaObject.required.getOrElse(List.empty[String])
    val newType: Option[NewType] = schemaObject.`type`.get match {
      case SchemaObjectType.`object` =>
        loadObjectProperties(
          name,
          schemaObject.properties.getOrElse(Map.empty),
          required,
          schemaObject.summary,
          schemaObject.description,
          allSchemas
        )
      case SchemaObjectType.`string` =>
        schemaObject.enum.map(loadEnum(name, _, schemaObject.summary, schemaObject.description))
      case SchemaObjectType.`integer` =>
        schemaObject.enum.map(loadEnum(name, _, schemaObject.summary, schemaObject.description))
      case SchemaObjectType.`array` =>
        throw new AssertionError(s"Top-level (components/schemas) schemas should not be arrays ($name : $schemaObject)")
      case SchemaObjectType.`dict` =>
        throw new AssertionError(s"Top-level (components/schemas) schemas should not be dicts ($name : $schemaObject)")
      case SchemaObjectType.`boolean` =>
        throw new AssertionError(
          s"Top-level (components/schemas) schemas should not be booleans ($name : $schemaObject)"
        )
      case SchemaObjectType.`number` =>
        throw new AssertionError(
          s"Top-level (components/schemas) schemas should not be numbers ($name : $schemaObject)"
        )
    }
    newType.map(NewTypeSymbol(name, _))
  }

  private def handleSchemaObjectUnionType(
      name: String,
      discriminator: Option[Discriminator],
      unionMembers: List[ReferenceObject],
      summary: Option[String],
      description: Option[String]
  )(
      implicit packageName: PackageName
  ): Option[Symbol] = {
    val references: Set[Ref] = unionMembers.map {
      case ReferenceObject(ref, _) =>
        val name: String = ref.split("/").last
        val path: String = ref.split("/").dropRight(1).mkString(".")
        Ref(path, name, None)
    }.toSet
    val newType: NewType =
      PrimitiveUnion(
        packageName.name,
        name,
        values = references,
        discriminator
          .map(_.propertyName)
          .getOrElse(
            throw new AssertionError(
              "We require that a discriminator be defined to use oneOf in OpenAPI. Checkout oneOf in OpenAPI 3.0 spec for more information."
            )
          ),
        summary,
        description
      )
    NewTypeSymbol(name, newType).some
  }

  private def evalSchema(name: String, schemaObject: SchemaObject, allSchemas: Map[String, SchemaObject])(
      implicit packageName: PackageName
  ): Option[Symbol] =
    schemaObject.oneOf.fold(handleSchemaObjectProductType(name, schemaObject, allSchemas))(
      handleSchemaObjectUnionType(name, schemaObject.discriminator, _, schemaObject.summary, schemaObject.description)
    )

  private def schemaObjectHasReadOnlyComponent(components: Map[String, SchemaObject])(
      schemaOrReferenceObject: SchemaOrReferenceObject
  ): Boolean = schemaOrReferenceObject match {
    case so: SchemaObject =>
      so.readOnly.getOrElse(
        so.properties.exists { _.values.exists { schemaObjectHasReadOnlyComponent(components) } }
      ) || so.oneOf.fold(false)(_.map(schemaObjectHasReadOnlyComponent(components)(_)).reduce(_ || _))
    case ReferenceObject(ref, _) =>
      schemaObjectHasReadOnlyComponent(components)(findRefInComponents(components, ref.split("/").last))
  }

  private def hasReadOnlyBase: SchemaOrReferenceObject => Boolean = {
    case so: SchemaObject      => so.readOnly.getOrElse(false)
    case ReferenceObject(_, _) => false
  }

  private def enrichSublevelPropsWithRequest(
      stringToObject: Map[String, SchemaOrReferenceObject]
  )(components: Map[String, SchemaObject]): Map[String, SchemaOrReferenceObject] =
    stringToObject
      .map {
        case (k, v: ReferenceObject) =>
          if (schemaObjectHasReadOnlyComponent(components)(v)) {
            (k, ReferenceObject(v.$ref + "Request", v.default))
          } else {
            (k, v)
          }
        case z => z
      }

  private[ast] def splitReadOnlyComponents(components: Map[String, SchemaObject]): Map[String, SchemaObject] = {
    val (hasReadOnlyProps: Map[String, SchemaObject], hasNoReadOnlyProps: Map[String, SchemaObject]) =
      components.partition(x => schemaObjectHasReadOnlyComponent(components)(x._2))
    val withRequestPostfix = hasReadOnlyProps.map {
      case (typeName: String, sor: SchemaOrReferenceObject) =>
        s"${typeName}Request" -> sor.copy(
          properties = sor.properties.map(
            (x: Map[String, SchemaOrReferenceObject]) =>
              enrichSublevelPropsWithRequest(x.filterNot {
                case (_, v) =>
                  hasReadOnlyBase(v)
              })(components)
          ),
          oneOf = sor.oneOf.map(
            _.map(
              ref =>
                if (schemaObjectHasReadOnlyComponent(components)(ref)) ref.copy($ref = ref.$ref + "Request")
                else ref
            )
          )
        )
    }
    hasNoReadOnlyProps ++ hasReadOnlyProps ++ withRequestPostfix
  }

  def readComponentsToInterop(ast: CoreASTRepr)(implicit packageName: PackageName): Map[String, Symbol] =
    splitReadOnlyComponents(ast.components.schemas)
      .flatMap {
        case (name: String, schemaObject: SchemaObject) =>
          evalSchema(name, schemaObject, ast.components.schemas)
            .map { cleanFilename(name) -> _ }
      }
      .toList
      .toMap

  private def aggregateRoutes(routes: List[RouteDefinition]): Map[String, PathItemAggregation] =
    routes
      .foldLeft(Map.empty[String, List[RouteDefinition]]) {
        case (acc, route) =>
          val existingRoutes = acc.getOrElse(route.path, List.empty)
          val nextRoute      = existingRoutes :+ route
          acc + (route.path -> nextRoute)
      }
      .map {
        case (key, value) => cleanFilename(key) -> PathItemAggregation(key, value)
      }

  def readRoutesToInterop(ast: CoreASTRepr)(implicit packageName: PackageName): Map[String, PathItemAggregation] =
    ast.paths.map(p => aggregateRoutes(readToRoutes(p)(ast.components.schemas))).getOrElse(Map.empty)

}
