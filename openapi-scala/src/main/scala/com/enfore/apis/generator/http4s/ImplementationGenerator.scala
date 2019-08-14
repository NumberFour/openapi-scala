package com.enfore.apis.generator.http4s

import com.enfore.apis.generator.ShowTypeTag._
import com.enfore.apis.repr.ReqWithContentType.{POST, PUT}
import com.enfore.apis.repr._
import com.enfore.apis.repr.TypeRepr._

object ImplementationGenerator {

  import com.enfore.apis.generator.Utilities._

  private type ArgumentName = String
  private type ArgumentType = String

  def generate(route: RouteDefinition): String = {
    val functionName = route.operationId.getOrElse(s"`${getFunctionName(route)}`")
    val parameters   = getParameters(route)
    val responseType = getResponseType(route)
    val docs = route.summary
      .map(_.split("\n").map(" * " + _).mkString("/**\n", "\n", "\n**/"))
      .map(_ + "\n")
      .getOrElse("")

    s"""${docs}def $functionName$parameters(implicit request: Request[F]): F[$responseType]""".stripMargin
  }

  /**
    * Returns the generated name for a method and URL combination without backticks, e.g. "GET /contacts/{contact-id}"
    */
  def getFunctionName(route: RouteDefinition): String = s"${getMethod(route)} ${route.path}"

  private def getMethod(route: RouteDefinition): String = route match {
    case _: GetRequest                                     => "GET"
    case PutOrPostRequest(_, _, _, PUT, _, _, _, _, _, _)  => "PUT"
    case PutOrPostRequest(_, _, _, POST, _, _, _, _, _, _) => "POST"
    case _: DeleteRequest                                  => "DELETE"
  }

  private def getParameters(route: RouteDefinition): String = {
    val pathParameters  = getPathParameters(route)
    val queryParameters = getQueryParams(route)
    val body            = getRequestBody(route)

    val allParameters = pathParameters ++ queryParameters ++ body

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
      case GetRequest(_, _, _, _, _, response, _)                => getJsonOrFirstType(response)
      case PutOrPostRequest(_, _, _, _, _, _, _, response, _, _) => getJsonOrFirstType(response)
      case _: DeleteRequest                                      => None
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

    extractFromPathParameters(route.pathParams)
  }

  private def getQueryParams(route: RouteDefinition): List[(ArgumentName, ArgumentType)] = {
    def extractFromQueries(queries: Map[String, Primitive]): List[(String, String)] =
      queries.mapValues(primitiveShowType.showType).toList.sortBy(_._1)

    route match {
      case GetRequest(_, _, _, _, queries, _, _)                => extractFromQueries(queries)
      case PutOrPostRequest(_, _, _, _, _, queries, _, _, _, _) => extractFromQueries(queries)
      case DeleteRequest(_, _, _, _, _, _)                      => List.empty
    }
  }

  private def getRequestBody(route: RouteDefinition): Option[(ArgumentName, ArgumentType)] = route match {
    case req: PutOrPostRequest =>
      req.readOnlyTypeName.map(ro => ("body", ro))
    case _: Any => None
  }
}
