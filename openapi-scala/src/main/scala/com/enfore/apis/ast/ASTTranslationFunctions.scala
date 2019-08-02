package com.enfore.apis.ast

import cats.implicits._
import com.enfore.apis.ast.SwaggerAST.RequestType.{POST, PUT}
import com.enfore.apis.ast.SwaggerAST._
import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr._

import scala.collection.immutable

object ASTTranslationFunctions {

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
        case _: SchemaObject      => None
        case ReferenceObject(ref) => Some(ref)
      }
      .map(x => Ref(x, x.split("/").last))

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
    case POST => ReqWithContentType.POST
    case PUT  => ReqWithContentType.PUT
    case _    => ReqWithContentType.POST
  }

  private def extractPathParameters(parameters: List[SwaggerAST.ParameterObject]): List[PathParameter] =
    parameters.filter(_.in == ParameterLocation.path).map(x => PathParameter(x.name))

  private def extractQueryParameters(
      parameters: List[SwaggerAST.ParameterObject]
  )(implicit packageName: PackageName): Map[String, Primitive] =
    parameters
      .filter(_.in == ParameterLocation.query)
      .map(
        (y: ParameterObject) =>
          y.name -> {
            y.schema.get match {
              case a: SchemaObject =>
                val isRequired: Boolean = y.required.getOrElse(false)
                // TODO: META-6088 Add support for refinements here
                val value: Primitive =
                  buildPrimitiveFromSchemaObjectType(refinements = None, a.items)(packageName)(a.`type`.get).get
                if (!isRequired) {
                  PrimitiveOption(value, None)
                } else {
                  value
                }
              case ReferenceObject(_) =>
                throw new NotImplementedError("ReferenceObjects in query parameters are not supported.")
            }
        }
      )
      .toMap

  private def findRefInComponents(
      components: Map[String, SchemaObject],
      typeName: String,
      context: String = ""): SchemaObject =
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

  private def routeDefFromSwaggerAST(path: String)(route: OperationObject, requestType: RequestType)(
      components: Map[String, SchemaObject]
  )(implicit packageName: PackageName): RouteDefinition = {
    val parameters                          = route.parameters.getOrElse(List.empty[ParameterObject])
    val pathParameters: List[PathParameter] = extractPathParameters(parameters)
    val queryParams: Map[String, Primitive] = extractQueryParameters(parameters)
    val possibleBodies: List[TypeRepr]      = route.requestBody.toList.flatMap(getBodyEncodings)
    val possibleResponse: Option[(Int, Map[String, Ref])] =
      getNameContentEncoding(route.responses, httpStatus => httpStatus >= 200 && httpStatus < 300)
    assert(possibleResponse.nonEmpty, "There has to be one successful (>=200 and <300) return code")
    requestType match {
      case RequestType.POST | RequestType.PUT =>
        PutOrPostRequest(
          path = path,
          summary = route.summary,
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
        val put: Option[RouteDefinition] =
          routes.get("put").map(routeDefFromSwaggerAST(path)(_, RequestType.PUT)(components))
        val post: Option[RouteDefinition] =
          routes.get("post").map(routeDefFromSwaggerAST(path)(_, RequestType.POST)(components))
        val get: Option[RouteDefinition] =
          routes.get("get").map(routeDefFromSwaggerAST(path)(_, RequestType.GET)(components))
        val delete: Option[RouteDefinition] =
          routes.get("delete").map(routeDefFromSwaggerAST(path)(_, RequestType.DELETE)(components))
        List(put, post, get, delete).flatten
    }.toList

  //  Functions for translating components

  private val illegalFilePathSymbols = List("/")

  private def cleanFilename(name: String): String =
    illegalFilePathSymbols.foldLeft(name) {
      case (filename: String, symbol: String) => filename.replaceAll(symbol, "_")
    }

  private def buildPrimitiveFromSchemaObjectTypeForComponents(
      items: Option[SchemaOrReferenceObject],
      refinements: Option[List[RefinedTags]]
  )(
      implicit packageName: PackageName
  ): SchemaObjectType => Option[TypeRepr] = {
    case schemaObject @ SchemaObjectType.`object` =>
      println(s"Ignoring inline object: $schemaObject")
      None
    case SchemaObjectType.array =>
      assert(items.isDefined, "A parameter type for an array is not defined")
      loadSingleProperty(items.get).map(PrimitiveArray(_, refinements))
    case x => buildPrimitiveFromSchemaObjectType(refinements)(packageName)(x)
  }

  private def buildPrimitiveFromSchemaObjectType(
      refinements: Option[List[TypeRepr.RefinedTags]],
      items: Option[SchemaOrReferenceObject] = None
  )(implicit packageName: PackageName): SchemaObjectType => Option[Primitive] = {
    case SchemaObjectType.string    => Some(PrimitiveString(refinements))
    case SchemaObjectType.boolean   => Some(PrimitiveBoolean(refinements))
    case SchemaObjectType.number    => Some(PrimitiveNumber(refinements))
    case SchemaObjectType.`integer` => Some(PrimitiveInt(refinements))
    case SchemaObjectType.`array` =>
      val someType: Option[TypeRepr] = items.flatMap {
        case so: SchemaObject =>
          so.`type`.flatMap(sot => buildPrimitiveFromSchemaObjectType(refinements)(packageName)(sot))
        case ro: ReferenceObject =>
          loadSingleProperty(ro)
      }
      Some(PrimitiveArray(someType.get, None))
    case x => {
      println(s"Ignoring '$x' which is not supported in buildPrimitiveFromSchemaObjectType")
      None
    }
  }

  private def loadSingleProperty(
      property: SchemaOrReferenceObject
  )(implicit packageName: PackageName): Option[TypeRepr] =
    property match {
      case so: SchemaObject =>
        val refinements: Option[List[CollectionRefinements]] =
          List(so.minLength.map(MinLength), so.maxLength.map(MaxLength))
            .filter(_.isDefined)
            .sequence
        so.`type`.flatMap(buildPrimitiveFromSchemaObjectTypeForComponents(so.items, refinements))
      case ReferenceObject(ref) => {
        val name: String = ref.split("/").last
        val path: String = ref.split("/").dropRight(1).mkString(".")
        (Ref(path.replace("#.components.schemas", packageName.name), name): TypeRepr).some
      }
    }

  private def makeSymbolFromTypeRepr(name: String, repr: TypeRepr): Symbol = repr match {
    case p: Primitive => PrimitiveSymbol(name, p)
    case n: NewType   => NewTypeSymbol(name, n)
    case r: Ref       => RefSymbol(name, r)
  }

  private def loadObjectProperties(
      typeName: String,
      properties: Map[String, SchemaOrReferenceObject],
      required: List[String]
  )(
      implicit packageName: PackageName
  ): Option[NewType] = {
    val mapped: immutable.Iterable[Option[Symbol]] = properties.mapValues(loadSingleProperty) map {
      case (name: String, repr: Option[TypeRepr]) =>
        val mapOp: Option[TypeRepr] = if (required.contains(name)) repr else repr.map(PrimitiveOption(_, None))
        assert(mapOp.isDefined, s"$name in $typeName could not be parsed.")
        mapOp map (makeSymbolFromTypeRepr(name, _))
    }
    mapped.toList.sequence.map(PrimitiveProduct(packageName.name, typeName, _))
  }

  private def loadEnum(typeName: String, values: List[String])(implicit packageName: PackageName): NewType =
    PrimitiveEnum(packageName.name, typeName, values.toSet)

  private def evalSchema(name: String, schemaObject: SchemaObject)(
      implicit packageName: PackageName
  ): Option[Symbol] = {
    val required = schemaObject.required.getOrElse(List.empty[String])
    val newType: Option[NewType] = schemaObject.`type`.get match {
      case SchemaObjectType.`object`  => schemaObject.properties.flatMap(loadObjectProperties(name, _, required))
      case SchemaObjectType.`string`  => schemaObject.enum.map(loadEnum(name, _))
      case SchemaObjectType.`integer` => schemaObject.enum.map(loadEnum(name, _))
      case SchemaObjectType.`array` =>
        throw new AssertionError(s"Top-level (components/schemas) schemas should not be arrays ($name : $schemaObject)")
      case SchemaObjectType.`boolean` =>
        throw new AssertionError(
          s"Top-level (components/schemas) schemas should not be booleans ($name : $schemaObject)")
      case SchemaObjectType.`number` =>
        throw new AssertionError(
          s"Top-level (components/schemas) schemas should not be numbers ($name : $schemaObject)")
    }
    newType.map(NewTypeSymbol(name, _))
  }

  private def schemaObjectHasReadOnlyComponent(components: Map[String, SchemaObject])(
      schemaOrReferenceObject: SchemaOrReferenceObject
  ): Boolean = schemaOrReferenceObject match {
    case so: SchemaObject =>
      so.readOnly.getOrElse(
        so.properties.exists { _.values.exists { schemaObjectHasReadOnlyComponent(components) } }
      )
    case ReferenceObject(ref) =>
      schemaObjectHasReadOnlyComponent(components)(findRefInComponents(components, ref.split("/").last))
  }

  private def hasReadOnlyBase: SchemaOrReferenceObject => Boolean = {
    case so: SchemaObject   => so.readOnly.getOrElse(false)
    case ReferenceObject(_) => false
  }

  private def enrichSublevelPropsWithRequest(
      stringToObject: Map[String, SchemaOrReferenceObject]
  )(components: Map[String, SchemaObject]): Map[String, SchemaOrReferenceObject] =
    stringToObject
      .map {
        case (k, v: ReferenceObject) =>
          if (schemaObjectHasReadOnlyComponent(components)(v)) {
            (k, ReferenceObject(v.$ref + "Request"))
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
          )
        )
    }
    hasNoReadOnlyProps ++ hasReadOnlyProps ++ withRequestPostfix
  }

  def readComponentsToInterop(ast: CoreASTRepr)(implicit packageName: PackageName): Map[String, Symbol] =
    splitReadOnlyComponents(ast.components.schemas)
      .flatMap {
        case (name: String, schemaObject: SchemaObject) =>
          evalSchema(name, schemaObject)
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
