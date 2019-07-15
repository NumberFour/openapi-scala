package com.enfore.apis.generator.http4s

import com.enfore.apis.repr._
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.generator.ShowTypeTag._
import com.enfore.apis.generator.ShowTypeTag.ops._
import com.enfore.apis.repr.ReqWithContentType.{POST, PUT}

object RouteGenerator {
  import com.enfore.apis.generator.Utilities._

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
    * Array query string is assumed to be split with comma, i.e. ?q=a,b,c,d
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

    if (routes.exists(anyListQueryParameter)) {

      val refDecoders =
        routes
          .flatMap(getEnumListQueryParameter)
          .flatten
          .flatMap(typeName => enumDecoder(typeName, indentationLevel))
      if (refDecoders.nonEmpty) {
        println("Generating http4s decoders for request references, assuming they all are enums")
        val importLine = List("import org.http4s.QueryParamDecoder.fromUnsafeCast\n")
        (importLine ++ refDecoders ++ decoder).map(`\t` * indentationLevel + _)
      } else {
        decoder.map(`\t` * indentationLevel + _)
      }
    } else {
      Nil
    }
  }

  /**
    *  Generating http4s decoders for request enumerations
    *
    * @param enumFullTypeName full enum name including package
    * @param indentationLevel
    *
    * @return generated decoder string lines
    */
  def enumDecoder(enumFullTypeName: String, indentationLevel: Int): List[String] = {
    val typeNameLowerCase = enumFullTypeName.toLowerCase
      .split("\\.")
      .last
    s"""implicit lazy val ${typeNameLowerCase}QueryParamDecoder: QueryParamDecoder[$enumFullTypeName] =
          fromUnsafeCast[$enumFullTypeName](x => $enumFullTypeName.withName(x.value))("$enumFullTypeName enum")
    """.stripMargin
      .split('\n')
      .toList
      .map(_ + `\t` * indentationLevel)
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
      case a: PutOrPostRequest =>
        a.readOnlyTypeName.map(x => s"request.as[$x]").getOrElse("")
      case _ => ""
    }

    val functionName = ImplementationGenerator.getFunctionName(route)

    def flatmapPutOrPost(s: String) = route match {
      case p: PutOrPostRequest if p.request.nonEmpty => s".flatMap($s)"
      case _                                         => s
    }

    val arguments = getArgumentList(route) + "(request)"

    val status = getResponseStatus(route)

    s"""errorHandler.resolve($requestDecoding${flatmapPutOrPost(s"impl.`$functionName`$arguments")}, $status)"""
  }

  private val anyListQueryParameter: RouteDefinition => Boolean = {
    getListQueryParams(_).values.toList.exists(listParameterType)
  }

  private val getEnumListQueryParameter: RouteDefinition => List[Option[String]] = {
    getListQueryParams(_).values.toList.map(enumListParameterType)
  }

  private val getListQueryParams: RouteDefinition =/> Map[String, TypeRepr.Primitive] = {
    case GetRequest(_, _, queries, _, _)                => queries
    case PutOrPostRequest(_, _, _, queries, _, _, _, _) => queries
    case _: DeleteRequest                               => Map.empty
  }

  private val enumListParameterType: TypeRepr.Primitive =/> Option[String] = {
    case PrimitiveArray(ref: Ref, _)                     => Some(ref.path + "." + ref.typeName)
    case PrimitiveOption(PrimitiveArray(ref: Ref, _), _) => Some(ref.path + "." + ref.typeName)
    case _: Any                                          => None
  }

  private val listParameterType: TypeRepr.Primitive =/> Boolean = {
    case _: PrimitiveArray                     => true
    case PrimitiveOption(_: PrimitiveArray, _) => true
    case _: Any                                => false
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
    case PrimitiveOption(dataType, _) => s"OptionalQueryParamDecoderMatcher[${dataType.showType}]"
    case other                        => s"QueryParamDecoderMatcher[${other.showType}]"
  }

  private def getArgumentList(route: RouteDefinition): String = {
    def buildParameters(fromPath: List[PathParameter], fromQueries: List[String]): List[String] =
      (fromPath.map(_.name) ++ fromQueries.sorted).map(sanitiseScalaName)
    def applyOrNot(requestNonEmpty: Boolean): String =
      if (requestNonEmpty) {
        "(_)"
      } else {
        ""
      }

    route match {
      case GetRequest(_, Nil, queries, _, _) if queries.isEmpty => ""
      case GetRequest(_, parameters, queries, _, _) =>
        buildParameters(parameters, queries.keys.toList).mkString("(", ", ", ")")
      case PutOrPostRequest(_, _, Nil, queries, request, _, _, _) if queries.isEmpty => applyOrNot(request.nonEmpty)
      case PutOrPostRequest(_, _, parameters, queries, request, _, _, _) =>
        (buildParameters(parameters, queries.keys.toList) ++ (if (request.nonEmpty) List("_") else List.empty))
          .mkString("(", ", ", ")")
      case DeleteRequest(_, Nil, _, _)        => ""
      case DeleteRequest(_, parameters, _, _) => buildParameters(parameters, List.empty).mkString("(", ", ", ")")
    }
  }

  private def getVariableNameAndMethod(route: RouteDefinition): String =
    route match {
      case _: GetRequest                               => "request @ GET"
      case PutOrPostRequest(_, PUT, _, _, _, _, _, _)  => "request @ PUT"
      case PutOrPostRequest(_, POST, _, _, _, _, _, _) => "request @ POST"
      case _: DeleteRequest                            => "request @ DELETE"
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

  private def buildString(response: Option[Map[String, TypeRepr.Ref]], status: Int): String =
    if (response.isEmpty) {
      s"(_: ${firstResponseType(response)}) => EmptyGenerator($status)()"
    } else {
      s"(x: ${firstResponseType(response)}) => EntityGenerator($status)(x.asJson)"
    }

  private def getResponseStatus(route: RouteDefinition): String =
    route match {
      case DeleteRequest(_, _, response, status)                => buildString(response, status)
      case PutOrPostRequest(_, _, _, _, _, response, _, status) => buildString(response, status)
      case GetRequest(_, _, _, response, status)                => buildString(response, status)
    }
}
