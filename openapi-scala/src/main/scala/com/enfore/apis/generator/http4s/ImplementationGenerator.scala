package com.enfore.apis.generator.http4s

import com.enfore.apis.generator.SymbolAnnotationMaker
import com.enfore.apis.repr.ReqWithContentType.{PATCH, POST, PUT}
import com.enfore.apis.repr._
import com.enfore.apis.repr.TypeRepr._

object ImplementationGenerator {

  import com.enfore.apis.generator.Utilities._

  final case class Argument(name: String, typeName: String)

  object Argument {
    val fromTuple = (Argument.apply _).tupled
  }

  /**
    * Indentation as a Variable (iaav)
    */
  private val `\t` = " " * 2

  def generate(route: RouteDefinition, indentationLevel: Int): List[String] = {
    val functionName: String = route.operationId.getOrElse(s"`${getFunctionName(route)}`")
    val parameters: String   = getParameters(route)
    val responseType: String = getResponseType(route)
    val summaryDocs: List[String] = route.summary
      .map(_.split("\n").toList.map(l => s" * $l"))
      .getOrElse(Nil)

    val descriptionDocs = route.description
      .map(_.split("\n").toList.map(l => s" * $l"))
      .getOrElse(Nil)

    val docs: List[String] =
      if ((summaryDocs ++ descriptionDocs).isEmpty) Nil
      else ((("/**" +: summaryDocs) ++ descriptionDocs) :+ "**/")

    (docs :+ s"""def $functionName$parameters(implicit request: Request[F]): F[$responseType]""")
      .map(`\t` * indentationLevel + _)
  }

  /**
    * Returns the generated name for a method and URL combination without backticks, e.g. "GET /contacts/{contact-id}"
    */
  def getFunctionName(route: RouteDefinition): String = s"${getMethod(route)} ${route.path}"

  private def getMethod(route: RouteDefinition): String = route match {
    case _: GetRequest                                           => "GET"
    case RequestWithPayload(_, _, _, _, PUT, _, _, _, _, _, _)   => "PUT"
    case RequestWithPayload(_, _, _, _, POST, _, _, _, _, _, _)  => "POST"
    case RequestWithPayload(_, _, _, _, PATCH, _, _, _, _, _, _) => "PATCH"
    case _: DeleteRequest                                        => "DELETE"
  }

  private def getParameters(route: RouteDefinition): String = {
    val pathParameters: List[Argument]  = getPathParameters(route)
    val queryParameters: List[Argument] = getQueryParams(route)
    val body: Option[Argument]          = getRequestBody(route)

    val allParameters = pathParameters ++ queryParameters ++ body

    assert(
      allParameters.map(_.name).size == allParameters.map(_.name).distinct.size,
      "Parameter names must be unique in path and query parameters altogether and must not be name 'body'"
    )

    val parametersAsString =
      allParameters
        .map { case Argument(key, value) => cleanScalaSymbol(key) -> value }
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
      case GetRequest(_, _, _, _, _, _, response, _)                  => getJsonOrFirstType(response)
      case RequestWithPayload(_, _, _, _, _, _, _, _, response, _, _) => getJsonOrFirstType(response)
      case _: DeleteRequest                                           => None
    }

    responseType.fold("Unit")(_.typeName)
  }

  private def getPathParameters(route: RouteDefinition): List[Argument] = {
    def extractFromPathParameters(parameters: List[PathParameter]): List[Argument] = {
      assert(
        parameters.map(_.name).size == parameters.map(_.name).distinct.size,
        "Path parameter names must be unique"
      )

      parameters.map(param => (param.name, "String"))
    } map Argument.fromTuple

    extractFromPathParameters(route.pathParams)
  }

  private def getQueryParams(route: RouteDefinition): List[Argument] = {
    def extractFromQueries(queries: Map[String, TypeRepr]): List[Argument] =
      queries
        .mapValues(SymbolAnnotationMaker.primitiveTypeSigWithRefinements)
        .toList
        .sortBy(_._1)
        .map(Argument.fromTuple)

    route match {
      case GetRequest(_, _, _, _, _, queries, _, _)                  => extractFromQueries(queries)
      case RequestWithPayload(_, _, _, _, _, _, queries, _, _, _, _) => extractFromQueries(queries)
      case DeleteRequest(_, _, _, _, _, _, _)                        => List.empty
    }
  }

  private def getRequestBody(route: RouteDefinition): Option[Argument] = route match {
    case req: RequestWithPayload =>
      req.readOnlyTypeName.map(ro => ("body", ro)).map(Argument.fromTuple)
    case _: Any => None
  }
}
