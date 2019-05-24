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

  private def extractRefOfMediaTypeObject(schemaObject: MediaTypeObject): Option[Ref] =
    schemaObject.schema
      .flatMap {
        case _: SchemaObject      => None
        case ReferenceObject(ref) => Some(ref)
      }
      .map(x => Ref(x, x.split("/").last))

  private def getEncodings(media: RequestBodyObject): List[Ref] =
    media.content.values.toList.map { extractRefOfMediaTypeObject }.sequence.toList.flatten

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

  private def extractQueryParameters(parameters: List[SwaggerAST.ParameterObject]): Map[String, Primitive] =
    parameters
      .filter(_.in == ParameterLocation.query)
      .map((y: ParameterObject) =>
        y.name -> {
          y.schema.get match {
            case a: SchemaObject =>
              val isRequired: Boolean = y.required.getOrElse(false)
              // TODO: META-6088 Add support for refinements here
              val value: Primitive = buildPrimitiveFromSchemaObjectType(refinements = None)(a.`type`.get).get
              if (!isRequired) {
                PrimitiveOption(value, None)
              } else {
                value
              }
            case ReferenceObject(_) =>
              throw new NotImplementedError("ReferenceObjects in query parameters are not supported.")
          }
      })
      .toMap

  private def routeDefFromSwaggerAST(path: String)(route: OperationObject, requestType: RequestType)(
      components: Map[String, SchemaObject]
  ): RouteDefinition = {
    val parameters                          = route.parameters.getOrElse(List.empty[ParameterObject])
    val pathParameters: List[PathParameter] = extractPathParameters(parameters)
    val queryParams: Map[String, Primitive] = extractQueryParameters(parameters)
    val possibleBodies: List[Ref]           = route.requestBody.toList.flatMap(getEncodings)
    val possibleResponse: Option[(Int, Map[String, Ref])] =
      getNameContentEncoding(route.responses, x => x >= 200 && x < 300)
    assert(possibleResponse.nonEmpty, "There has to be one successful (>=200 and <300) return code")
    requestType match {
      case RequestType.POST | RequestType.PUT =>
        assert(
          possibleBodies.size == 1,
          s"We only support exactly one content type for '$path':'$requestType' request. Found: ${possibleBodies}")
        val correspondingComponent: Option[SchemaObject] = components
          .find {
            case (key: String, _: SchemaObject) =>
              key == possibleBodies.head.typeName
          }
          .map(_._2)
        assert(
          correspondingComponent.nonEmpty,
          s"The component referenced in $path $requestType (${possibleBodies.head.typeName}) is not found in components"
        )
        PutOrPostRequest(
          path = path,
          `type` = translateReqContentType(requestType),
          pathParams = pathParameters,
          queries = queryParams,
          request = possibleBodies.head,
          response = possibleResponse.map(_._2),
          hasReadOnlyType = hasReadOnlyComponent(correspondingComponent.get),
          successStatusCode = possibleResponse.get._1
        )
      case RequestType.GET =>
        GetRequest(
          path = path,
          pathParams = pathParameters,
          queries = queryParams,
          response = possibleResponse.map(_._2),
          successStatusCode = possibleResponse.get._1
        )
      case RequestType.DELETE =>
        DeleteRequest(
          path = path,
          pathParams = pathParameters,
          response = possibleResponse.map(_._2),
          successStatusCode = possibleResponse.get._1
        )
    }
  }

  private def readToRoutes(paths: Map[String, Map[String, OperationObject]])(
      components: Map[String, SchemaObject]
  ): List[RouteDefinition] =
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
    case schemaObject @ SchemaObjectType.`object` => {
      println(s"Ignoring inline object: ${schemaObject}")
      None
    }
    case SchemaObjectType.array =>
      assert(items.isDefined, "A parameter type for an array is not defined")
      loadSingleProperty(items.get).map(PrimitiveArray(_, refinements))
    case x => buildPrimitiveFromSchemaObjectType(refinements)(x)
  }

  private def buildPrimitiveFromSchemaObjectType(
      refinements: Option[List[TypeRepr.RefinedTags]]
  ): SchemaObjectType => Option[Primitive] = {
    case SchemaObjectType.string    => Some(PrimitiveString(refinements))
    case SchemaObjectType.boolean   => Some(PrimitiveBoolean(refinements))
    case SchemaObjectType.number    => Some(PrimitiveNumber(refinements))
    case SchemaObjectType.`integer` => Some(PrimitiveInt(refinements))
    case x => {
      println(s"Ignoring '$x' which is not supported in buildPrimitiveFromSchemaObjectType")
      None
    }
  }

  private def loadSingleProperty(property: SchemaOrReferenceObject)(
      implicit packageName: PackageName): Option[TypeRepr] =
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
      required: List[String])(
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
      implicit packageName: PackageName): Option[Symbol] = {
    val required = schemaObject.required.getOrElse(List.empty[String])
    val newType: Option[NewType] = schemaObject.`type`.get match {
      case SchemaObjectType.`object`  => schemaObject.properties.flatMap(loadObjectProperties(name, _, required))
      case SchemaObjectType.`string`  => schemaObject.enum.map(loadEnum(name, _))
      case SchemaObjectType.`integer` => schemaObject.enum.map(loadEnum(name, _))
      case SchemaObjectType.`array` => {
        throw new AssertionError("Top-level (components/schemas) schemas should not be arrays")
      }
      case SchemaObjectType.`boolean` => {
        throw new AssertionError("Top-level (components/schemas) schemas should not be booleans")
      }
      case SchemaObjectType.`number` => {
        throw new AssertionError("Top-level (components/schemas) schemas should not be numbers")
      }
    }
    newType.map(NewTypeSymbol(name, _))
  }

  private def hasReadOnlyComponent(schemaObject: SchemaObject): Boolean =
    schemaObject.properties.exists { _.values.exists { hasReadOnly } }

  private def hasReadOnly: SchemaOrReferenceObject => Boolean = {
    case so: SchemaObject   => so.readOnly.getOrElse(false)
    case ReferenceObject(_) => false
  }

  private[ast] def splitReadOnlyComponents(components: Map[String, SchemaObject]): Map[String, SchemaObject] = {
    val (hasReadOnlyProps: Map[String, SchemaObject], hasNoReadOnlyProps: Map[String, SchemaObject]) =
      components.partition(x => hasReadOnlyComponent(x._2))
    val withoutReadOnlyFields = hasReadOnlyProps.map {
      case (typeName: String, sor: SchemaOrReferenceObject) =>
        s"${typeName}Request" -> sor.copy(
          properties = sor.properties.map(
            (x: Map[String, SchemaOrReferenceObject]) =>
              x.filterNot {
                case (_, v) =>
                  hasReadOnly(v)
            }
          )
        )
    }
    hasNoReadOnlyProps ++ hasReadOnlyProps ++ withoutReadOnlyFields
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

  def readRoutesToInerop(ast: CoreASTRepr): Map[String, PathItemAggregation] =
    ast.paths.map(p => aggregateRoutes(readToRoutes(p)(ast.components.schemas))).getOrElse(Map.empty)

}
