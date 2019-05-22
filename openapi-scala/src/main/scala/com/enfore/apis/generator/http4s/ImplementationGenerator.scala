package com.enfore.apis.generator.http4s

import com.enfore.apis.generator.ScalaGenerator.cleanScalaSymbol
import com.enfore.apis.generator.ShowTypeTag._
import com.enfore.apis.repr.ReqWithContentType.{POST, PUT}
import com.enfore.apis.repr._
import com.enfore.apis.repr.TypeRepr._

object ImplementationGenerator {
  private type ArgumentName = String
  private type ArgumentType = String

  def generate(route: RouteDefinition): String = {
    val functionName = getFunctionName(route)
    val parameters   = getParameters(route)
    val responseType = getResponseType(route)

    s"""def `$functionName`$parameters(implicit request: Request[F]): F[$responseType]"""
  }

  /**
    * Returns the generated name for a method and URL combination without backticks, e.g. "GET /contacts/{contact-id}"
    */
  def getFunctionName(route: RouteDefinition): String = s"${getMethod(route)} ${route.path}"

  private def getMethod(route: RouteDefinition): String = route match {
    case _: GetRequest                            => "GET"
    case PutOrPostRequest(_, PUT, _, _, _, _, _)  => "PUT"
    case PutOrPostRequest(_, POST, _, _, _, _, _) => "POST"
    case _: DeleteRequest                         => "DELETE"
  }

  private def getParameters(route: RouteDefinition): String = {
    val pathParameters    = getPathParameters(route)
    val queryParamameters = getQueryParamameters(route)
    val body              = getRequestBody(route)

    val allParameters = pathParameters ++ queryParamameters ++ body

    assert(
      allParameters.map(_._1).size == allParameters.map(_._1).distinct.size,
      "Parameter names must be unique in path and query parameters altogether and must not be name 'body'"
    )

    val parametersAsString =
      allParameters
        .map { case (key, value) => cleanScalaSymbol(key) -> value }
        .map { case (name, dataType) => s"$name: $dataType" }
        .mkString("(", ", ", ")")

    if (allParameters.isEmpty) {
      ""
    } else {
      parametersAsString
    }
  }

  private def getResponseType(route: RouteDefinition): String = {
    def getJsonOrFirstType(response: Option[Map[String, Ref]]): Option[Ref] =
      response.flatMap(
        mediaTypes => mediaTypes.get("application/json").orElse(mediaTypes.headOption.map(_._2))
      )

    val responseType = route match {
      case GetRequest(_, _, _, response)                => getJsonOrFirstType(response)
      case PutOrPostRequest(_, _, _, _, _, response, _) => getJsonOrFirstType(response)
      case _: DeleteRequest                             => None
    }

    responseType.fold("Unit")(_.typeName)
  }

  private def getPathParameters(route: RouteDefinition): List[(ArgumentName, ArgumentType)] = {
    def extractFromPathParameters(parameters: List[PathParameter]): List[(String, String)] = {
      assert(
        parameters.map(_.name).size == parameters.map(_.name).distinct.size,
        "Path parameter names must be unique"
      )

      parameters.map(param => (param.name, "String"))
    }

    route match {
      case GetRequest(_, pathParams, _, _)                => extractFromPathParameters(pathParams)
      case PutOrPostRequest(_, _, pathParams, _, _, _, _) => extractFromPathParameters(pathParams)
      case DeleteRequest(_, pathParams, _)                => extractFromPathParameters(pathParams)
    }
  }

  private def getQueryParamameters(route: RouteDefinition): List[(ArgumentName, ArgumentType)] = {
    def extractFromQueries(queries: Map[String, Primitive]): List[(String, String)] =
      queries.mapValues(primitiveShowType.showType).toList.sortBy(_._1)

    route match {
      case GetRequest(_, _, queries, _)                => extractFromQueries(queries)
      case PutOrPostRequest(_, _, _, queries, _, _, _) => extractFromQueries(queries)
      case DeleteRequest(_, _, _)                      => List.empty
    }
  }

  private def getRequestBody(route: RouteDefinition): Option[(ArgumentName, ArgumentType)] = route match {
    case req: PutOrPostRequest =>
      Some(("body", req.readOnlyTypeName))
    case _: Any => None
  }
}
