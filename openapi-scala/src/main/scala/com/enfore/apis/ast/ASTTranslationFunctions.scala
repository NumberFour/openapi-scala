package com.enfore.apis.ast

import cats.implicits._
import com.enfore.apis.ast.SwaggerAST.RequestType.{POST, PUT}
import com.enfore.apis.ast.SwaggerAST._
import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr._

import scala.collection.immutable

object ASTTranslationFunctions {

  final case class PackageName(name: String)

  // Helper functions

  private def mapValues[K, V, X](in: Map[K, V])(f: V => X): Map[K, X] = in.map {
    case (key, value) => key -> f(value)
  }

  // Functions for translating routes

  private def buildRefFromSchemaRefContainer(schemaObject: SchemaRefContainer): Option[Ref] =
    schemaObject.schema
      .flatMap(_.$ref)
      .map(x => Ref(x, x.split("/").last))

  private def getEncodings(media: MediaTypeObject): List[Ref] =
    media.content.flatMap(_.values.toList.map { buildRefFromSchemaRefContainer }.sequence).toList.flatten

  private def getEncodedMap(media: MediaTypeObject): Map[String, Ref] =
    mapValues(
      mapValues(media.content.getOrElse(Map.empty))(buildRefFromSchemaRefContainer)
        .filter(_._2.isDefined)
    )(_.get)

  private def getNameContentEncoding(
      mediaMap: Map[Int, MediaTypeObject],
      acceptMedia: Int => Boolean
  ): Option[Map[String, Ref]] = mediaMap.find(x => acceptMedia(x._1)).map(x => getEncodedMap(x._2))

  // TODO: This is pretty hackey. Come up with a better solution for this
  private def translateReqContentType(in: RequestType): ReqWithContentType = in match {
    case POST => ReqWithContentType.POST
    case PUT  => ReqWithContentType.PUT
    case _    => ReqWithContentType.POST
  }

  private def extractPathParameters(parameters: List[SwaggerAST.ParamObject]) =
    parameters.filter(_.in == ParameterLocation.path).map(x => PathParameter(x.name))

  private def extractQueryParameters(parameters: List[ParamObject]): Map[String, Primitive] =
    parameters
      .filter(_.in == ParameterLocation.query)
      .map(_.name -> PrimitiveString(None)) // TODO: Will be extended later on
      .toMap

  private def routeDefFromSwaggerAST(path: String)(route: PathObject, requestType: RequestType): RouteDefinition = {
    val parameters                          = route.parameters.getOrElse(List.empty[ParamObject])
    val pathParameters: List[PathParameter] = extractPathParameters(parameters)
    val queryParams: Map[String, Primitive] = extractQueryParameters(parameters)
    val possibleBodies: List[Ref]           = route.requestBody.toList.flatMap(getEncodings)
    val possibleResponse: Option[Map[String, Ref]] =
      getNameContentEncoding(route.responses, x => x >= 200 && x < 300)
    requestType match {
      case RequestType.POST | RequestType.PUT =>
        assert(possibleBodies.size == 1, s"We only support one content type for $path $requestType request")
        PutOrPostRequest(
          path,
          translateReqContentType(requestType),
          pathParameters,
          queryParams,
          possibleBodies.head,
          possibleResponse
        )
      case RequestType.GET =>
        GetRequest(path, pathParameters, queryParams, possibleResponse)
      case RequestType.DELETE =>
        DeleteRequest(path, pathParameters, possibleResponse)
    }
  }

  private def readToRoutes(paths: Map[String, Map[String, PathObject]]): List[RouteDefinition] =
    paths.flatMap {
      case (path: String, routes: Map[String, PathObject]) =>
        val put: Option[RouteDefinition]  = routes.get("put").map(routeDefFromSwaggerAST(path)(_, RequestType.PUT))
        val post: Option[RouteDefinition] = routes.get("post").map(routeDefFromSwaggerAST(path)(_, RequestType.POST))
        val get: Option[RouteDefinition]  = routes.get("get").map(routeDefFromSwaggerAST(path)(_, RequestType.GET))
        val delete: Option[RouteDefinition] =
          routes.get("delete").map(routeDefFromSwaggerAST(path)(_, RequestType.DELETE))
        List(put, post, get, delete).flatten
    }.toList

  //  Functions for translating components

  val illegalFilePathSymbols = List("/")

  private def cleanFilename(name: String): String =
    illegalFilePathSymbols.foldLeft(name) {
      case (filename: String, symbol: String) => filename.replaceAll(symbol, "_")
    }

  def loadPropertyObject(pType: Option[PropertyType], param: Option[Property], refinements: Option[List[RefinedTags]])(
      implicit packageName: PackageName
  ): Option[TypeRepr] =
    pType flatMap {
      case PropertyType.`object` => None
      case PropertyType.string   => PrimitiveString(refinements).some
      case PropertyType.boolean  => PrimitiveBoolean(refinements).some
      case PropertyType.array =>
        assert(param.isDefined, "A parameter type for an array is not defined")
        loadSingleProperty(param.get).map(PrimitiveArray(_, refinements)).widen[TypeRepr]
      case PropertyType.number => PrimitiveNumber(refinements).some
    }

  def loadSingleProperty(property: Property)(implicit packageName: PackageName): Option[TypeRepr] = {
    val refinements: Option[List[CollectionRefinements]] =
      List(property.minLength.map(MinLength), property.maxLength.map(MaxLength)).filter(_.isDefined).sequence
    property.$ref.fold(loadPropertyObject(property.`type`, property.items, refinements)) { ref =>
      val name: String = ref.split("/").last
      val path: String = ref.split("/").dropRight(1).mkString(".")
      (Ref(path.replace("#.components.schemas", packageName.name), name): TypeRepr).some
    }
  }

  def makeSymbolFromTypeRepr(name: String, repr: TypeRepr): Symbol = repr match {
    case p: Primitive => PrimitiveSymbol(name, p)
    case n: NewType   => NewTypeSymbol(name, n)
    case r: Ref       => RefSymbol(name, r)
  }

  def loadObjectProperties(typeName: String, properties: Map[String, Property], required: List[String])(
      implicit packageName: PackageName
  ): Option[NewType] = {
    val mapped: immutable.Iterable[Option[Symbol]] = properties.mapValues(loadSingleProperty) map {
      case (name: String, repr: Option[TypeRepr]) =>
        val mapOp = if (required.contains(name)) repr else repr.map(PrimitiveOption)
        assert(mapOp.isDefined, s"$name in $typeName could not be parsed.")
        mapOp map (makeSymbolFromTypeRepr(name, _))
    }
    mapped.toList.sequence.map(PrimitiveProduct(packageName.name, typeName, _))
  }

  def loadEnum(typeName: String, values: List[String])(implicit packageName: PackageName): NewType =
    PrimitiveEnum(packageName.name, typeName, values.toSet)

  def evalSchema(name: String, component: Component)(implicit packageName: PackageName): Option[Symbol] = {
    val required = component.required.getOrElse(List.empty[String])
    val newType: Option[NewType] = component.`type` match {
      case ComponentType.`object` => component.properties.flatMap(loadObjectProperties(name, _, required))
      case ComponentType.`string` => component.enum.map(loadEnum(name, _))
    }
    newType.map(NewTypeSymbol(name, _))
  }

  def splitReadOnlyComponents(components: Map[String, Component]): Map[String, Component] = {
    val (hasReadOnlyProps: Map[String, Component], hasNoReadOnlyProps: Map[String, Component]) =
      components.partition(_._2.properties.exists { properties: Map[String, Property] =>
        properties.values.exists(_.readOnly.getOrElse(false))
      })
    val withoutReadOnlyFields = hasReadOnlyProps.map {
      case (cKey, cValue) =>
        s"${cKey}Request" -> cValue.copy(
          properties = cValue.properties.map(
            _.filterNot(
              _.exists(_.readOnly.getOrElse(false))
            )
          )
        )
    }
    hasNoReadOnlyProps ++ hasReadOnlyProps ++ withoutReadOnlyFields
  }

  def readComponentsToInterop(ast: CoreASTRepr)(implicit packageName: PackageName): Map[String, Symbol] =
    splitReadOnlyComponents(ast.components.schemas)
      .flatMap {
        case (name: String, component: Component) =>
          evalSchema(name, component)
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
    ast.paths.map(p => aggregateRoutes(readToRoutes(p))).getOrElse(Map.empty)

}
