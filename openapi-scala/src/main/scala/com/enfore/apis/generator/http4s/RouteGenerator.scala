package com.enfore.apis.generator.http4s

import com.enfore.apis.repr._
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.generator.ShowTypeTag._
import com.enfore.apis.generator.ShowTypeTag.ops._
import com.enfore.apis.generator.ScalaGenerator._
import com.enfore.apis.repr.ReqWithContentType.{POST, PUT}

object RouteGenerator {
  type =/>[A, B] = PartialFunction[A, B]

  /**
    * Indentation as a Variable (iaav)
    */
  private val `\t`                     = " " * 2
  private val pathVariableReqex        = "\\{(.*)}".r
  private val replaceLeadingSlashReqex = "^/"

  def generate(route: RouteDefinition, indentationLevel: Int): List[String] = {
    val url                = getRoute(route)
    val implementationCall = getImplementationCall(route)

    List(
      url,
      `\t` + implementationCall
    ).map(`\t` * indentationLevel + _)
  }

  /**
    * Get the list decoder matcher if there is any query parameter with list / array type.
    * Array querry string is assumed to be split with comma, i.e. ?q=a,b,c,d
    */
  def listDecoder(routes: List[RouteDefinition], indentationLevel: Int): List[String] = {
    val decoder =
      s"""implicit def listDecoder[T](implicit decoder: QueryParamDecoder[T]): QueryParamDecoder[List[T]] =
         |      QueryParamDecoder[String]
         |          .map(
         |            _.split(',').toList
         |                .flatMap(value => decoder.decode(QueryParameterValue(value)).toOption)
         |          )""".stripMargin
        .split('\n')
        .toList
        .map(_ + `\t` * indentationLevel)

    if (routes.exists(anyListQueryParameter)) {
      decoder
    } else {
      Nil
    }
  }

  /**
    * Build decoder matchers corresponding to the contained query parameter types
    */
  def buildMatchers(routes: List[RouteDefinition], indentationLevel: Int): List[String] = {
    val queryParameters = routes.map(getListQueryParams)

    queryParameters
      .flatMap(_.toList)
      .map(toMatcherTuple)
      .map {
        case (matcherName, matcherType) =>
          s"""object `$matcherName` extends $matcherType"""
      }
      .map(`\t` * indentationLevel + _)
      .distinct
  }

  /**
    * If the name is a Scala keyword the string "Parameter" will be appended to the name.
    * If the name contains symbols that are not allowed in Scala variable names, there will be an attempt to
    * convert those names to camel case. If that fails they'll be replaced by '_'.
    * Otherwise names will be returned without change
    */
  def sanitiseScalaName(name: String): String = {
    val notAScalaKeywordName =
      if (forbiddenSymbols.contains(name)) name + "Parameter"
      else name

    val nonSymbolName = illegalScalaSymbols.foldLeft(notAScalaKeywordName)((name, symbol) => name.replace(symbol, "_"))

    hyphenAndUnderscoreToCamel(nonSymbolName)
  }

  private def getRoute(route: RouteDefinition): String = {
    val variableAndMethod = getVariableNameAndMethod(route)

    val path = buildPath(route)

    val queryString = getQueryStringParameters(route)

    s"""case $variableAndMethod -> Root / $path$queryString =>"""
  }

  private def getImplementationCall(route: RouteDefinition): String = {
    val requestDecoding = route match {
      case PutOrPostRequest(_, _, _, _, request, _, hasReadOnlyType) =>
        if (hasReadOnlyType) {
          s"request.as[${request.typeName}Request]"
        } else {
          s"request.as[${request.typeName}]"
        }
      case _ => ""
    }

    val functionName = ImplementationGenerator.getFunctionName(route)

    def flatmapPutOrPost(s: String) = route match {
      case _: PutOrPostRequest => s".flatMap($s)"
      case _                   => s
    }

    val arguments = getArgumentList(route) + "(request)"

    val status = getResponseStatus(route)

    s"""errorHandler.resolve($requestDecoding${flatmapPutOrPost(s"impl.`$functionName`$arguments")}, $status)"""
  }

  private val anyListQueryParameter: RouteDefinition => Boolean = {
    getListQueryParams(_).values.toList.exists(listParameterType)
  }

  private val getListQueryParams: RouteDefinition =/> Map[String, TypeRepr.Primitive] = {
    case GetRequest(_, _, queries, _)                => queries
    case PutOrPostRequest(_, _, _, queries, _, _, _) => queries
    case _: DeleteRequest                            => Map.empty
  }

  private val listParameterType: TypeRepr.Primitive =/> Boolean = {
    case _: PrimitiveArray                  => true
    case PrimitiveOption(_: PrimitiveArray) => true
    case _: Any                             => false
  }

  /**
    * Returns the resulting decoder matchers name and extended type as a tuple.
    * The name comes without backtick. The extended type comes without "extends".
    * E.g.:
    * {{{
    *   (
    *     "query1 String Matcher",
    *     "QueryParamDecoderMatcher[String](\"query1\")"
    *   )
    * }}}
    */
  private val toMatcherTuple: (String, Primitive) =/> (String, String) = {
    case (paramName, paramType) =>
      (
        s"$paramName ${paramType.showType} Matcher",
        s"""${queryParameterDecoderMatcher(paramType)}("$paramName")"""
      )
  }

  private def queryParameterDecoderMatcher(dataType: Primitive): String = dataType match {
    case PrimitiveOption(dataType) => s"OptionalQueryParamDecoderMatcher[${dataType.showType}]"
    case other                     => s"QueryParamDecoderMatcher[${other.showType}]"
  }

  private def getArgumentList(route: RouteDefinition): String = {
    def buildParameters(fromPath: List[PathParameter], fromQueries: List[String]): List[String] =
      (fromPath.map(_.name) ++ fromQueries.sorted).map(sanitiseScalaName)

    route match {
      case GetRequest(_, Nil, queries, _) if queries.isEmpty =>
        ""

      case GetRequest(_, parameters, queries, _) =>
        buildParameters(parameters, queries.keys.toList).mkString("(", ", ", ")")

      case PutOrPostRequest(_, _, Nil, queries, request, _, _) if queries.isEmpty =>
        assert(request.typeName != "Unit", "Unit is not allowed as request type")
        "(_)"

      case PutOrPostRequest(_, _, parameters, queries, request, _, _) =>
        assert(request.typeName != "Unit", "Unit is not allowed as request type")
        (buildParameters(parameters, queries.keys.toList) :+ "_").mkString("(", ", ", ")")

      case DeleteRequest(_, Nil, _) =>
        ""

      case DeleteRequest(_, parameters, _) =>
        buildParameters(parameters, List.empty).mkString("(", ", ", ")")
    }
  }

  private def getVariableNameAndMethod(route: RouteDefinition): String =
    route match {
      case _: GetRequest                            => "request @ GET"
      case PutOrPostRequest(_, PUT, _, _, _, _, _)  => "request @ PUT"
      case PutOrPostRequest(_, POST, _, _, _, _, _) => "request @ POST"
      case _: DeleteRequest                         => "request @ DELETE"
    }

  private def buildPath(route: RouteDefinition): String =
    route.path
      .replaceAll(replaceLeadingSlashReqex, "")
      .split("/")
      .toList
      .map {
        case pathVariableReqex(variable) => sanitiseScalaName(variable)
        case pathString                  => "\"" + pathString + "\""
      }
      .mkString(" / ")

  private def getQueryStringParameters(route: RouteDefinition): String = {
    val queries = getListQueryParams(route).toList
      .map { case tuple @ (paramName, _) => paramName -> toMatcherTuple(tuple)._1 }
      .map { case (paramName, matcherName) => s"""`$matcherName`(${sanitiseScalaName(paramName)})""" }

    if (queries.isEmpty) {
      ""
    } else {
      queries.mkString(" :? ", " +& ", "")
    }
  }

  private def firstResponseType(response: Option[Map[String, Ref]]): String =
    response.flatMap(_.headOption).map(_._2.typeName).getOrElse("Unit")

  private def getResponseStatus(route: RouteDefinition): String =
    route match {
      // TODO: NoContent should follow from response code 204. Also support more than Ok
      case DeleteRequest(_, _, response) => s"(_: ${firstResponseType(response)}) => NoContent()"
      case PutOrPostRequest(_, _, _, _, _, response, _) =>
        s"(x: ${firstResponseType(response)}) => Ok(x.asJson)"
      case GetRequest(_, _, _, response) =>
        s"(x: ${firstResponseType(response)}) => Ok(x.asJson)"
    }
}
