package com.enfore.apis.generator.http4s

import com.enfore.apis.generator.{ScalaGenerator, SymbolAnnotationMaker}
import com.enfore.apis.repr._
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr.ReqWithContentType.{PATCH, POST, PUT}

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
    * Builds the implicit query parameter decoders for query parameters with refinements
    * so that matchers can be constructed for those refinement types.
    *
    * Examples include: {{{
    *   implicit lazy val `String Refined AllOf[...]` =
    *     QueryParameterDecoder[String].emap(input => (new RefinedTypeOps[String Refined AllOf[...], String]).emap(_)
    *       .left.map(err => ParsingError(err, new Exception("Failed to ..."))))
    * }}}
    *
    * In case of optional query parameters an example might include: {{{
    *   ... = OptionalQueryParameterDecoder[String].emap(...)
    * }}}
    *
    * @param in route definitions from which the query parameters must be extracted
    * @param indentationLevel of the created Scala code
    * @return A list of strings containing each line of Scala code to be injected
    */
  def buildRefinementDecoders(in: List[RouteDefinition], indentationLevel: Int): List[String] =
    in.flatMap(getQueryParams(_).values.toList)
      .flatMap {
        case PrimitiveOption(prim: Primitive, _) =>
          defineOptionalRefinementOnPrimitive(prim).map(
            refinementDecoder(_, prim.typeName, indentationLevel)
          )
        case x: Primitive =>
          defineOptionalRefinementOnPrimitive(x).map(
            refinementDecoder(_, x.typeName, indentationLevel)
          )
        case _ => None
      }
      .flatten

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

      val refDecoders: List[String] =
        routes
          .flatMap(getEnumQueryParameter)
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

  def refinementDecoder(
      refinementSyntax: String,
      baseTypeSig: String,
      indentationLevel: Int
  ): List[String] =
    s"""implicit lazy val `${refinementSyntax.replace('`', '_')}QueryParamDecoder`: QueryParamDecoder[$refinementSyntax] = 
       |  QueryParamDecoder[$baseTypeSig].emap((new RefinedTypeOps[$refinementSyntax, $baseTypeSig]).from(_).left.map(errMsg => ParseFailure("Failed to validate input query parameter", errMsg)))""".stripMargin
      .split('\n')
      .toList
      .map(_ + `\t` * indentationLevel)

  /**
    * Build decoder matchers corresponding to the contained query parameter types
    */
  def buildMatchers(routes: List[RouteDefinition], indentationLevel: Int): List[String] = {
    val queryParameters = routes.map(getQueryParams)

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
      case a: RequestWithPayload =>
        a.readOnlyTypeName.map(x => s"request.as[$x]").getOrElse("")
      case _ => ""
    }

    val functionName = route.operationId.getOrElse(s"`${ImplementationGenerator.getFunctionName(route)}`")

    def flatmapPutOrPost(s: String) = route match {
      case p: RequestWithPayload if p.request.nonEmpty => s".flatMap($s)"
      case _                                           => s
    }

    val arguments = getArgumentList(route) + "(request)"

    val status = getResponseStatus(route)

    s"""errorHandler.resolve($requestDecoding${flatmapPutOrPost(s"impl.$functionName$arguments")}, $status)"""
  }

  private val getQueryParams: RouteDefinition =/> Map[String, TypeRepr] = {
    case GetRequest(_, _, _, _, _, queries, _, _)                  => queries
    case RequestWithPayload(_, _, _, _, _, _, queries, _, _, _, _) => queries
    case _: DeleteRequest => {
      println("Warning: We are ignoring query parameters of DELETE requests")
      Map.empty
    }
  }

  private val isListParameterType: TypeRepr =/> Boolean = {
    case _: PrimitiveArray                     => true
    case PrimitiveOption(_: PrimitiveArray, _) => true
    case _: Any                                => false
  }

  private val getEnumParameterType: TypeRepr =/> Option[String] = {
    case ref: Ref                                        => Some(ref.path + "." + ref.typeName)
    case PrimitiveArray(ref: Ref, _)                     => Some(ref.path + "." + ref.typeName)
    case PrimitiveOption(ref: Ref, _)                    => Some(ref.path + "." + ref.typeName)
    case PrimitiveOption(PrimitiveArray(ref: Ref, _), _) => Some(ref.path + "." + ref.typeName)
    case _: Any                                          => None
  }

  private val anyListQueryParameter: RouteDefinition => Boolean = {
    getQueryParams(_).values.toList.exists(isListParameterType)
  }

  private val getEnumQueryParameter: RouteDefinition => List[Option[String]] = {
    getQueryParams(_).values.toList.map(getEnumParameterType)
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
  private val toMatcherTuple: (String, TypeRepr) =/> (String, String) = {
    case (paramName, paramType) =>
      (
        s"$paramName ${queryParameterDecoderMatcher(paramType).replace('`', '_')} Matcher",
        s"""${queryParameterDecoderMatcher(paramType)}("$paramName")"""
      )
  }

  def defineOptionalRefinementOnPrimitive(dataType: Primitive): Option[String] =
    ScalaGenerator.primitiveRefinementExtractor(dataType).map(SymbolAnnotationMaker.primitiveTypeSigWithRefinements)

  private def queryParameterDecoderMatcher(dataType: TypeRepr): String = dataType match {
    case PrimitiveOption(dataType, _) =>
      s"OptionalQueryParamDecoderMatcher[${SymbolAnnotationMaker.onlyResolverTopPrimitiveRefinements(dataType)}]"
    case other => s"QueryParamDecoderMatcher[${SymbolAnnotationMaker.onlyResolverTopPrimitiveRefinements(other)}]"
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
      case GetRequest(_, _, _, _, Nil, queries, _, _) if queries.isEmpty => ""
      case GetRequest(_, _, _, _, parameters, queries, _, _) =>
        buildParameters(parameters, queries.keys.toList).mkString("(", ", ", ")")
      case RequestWithPayload(_, _, _, _, _, Nil, queries, request, _, _, _) if queries.isEmpty =>
        applyOrNot(request.nonEmpty)
      case RequestWithPayload(_, _, _, _, _, parameters, queries, request, _, _, _) =>
        (buildParameters(parameters, queries.keys.toList) ++ (if (request.nonEmpty) List("_") else List.empty))
          .mkString("(", ", ", ")")
      case DeleteRequest(_, _, _, _, Nil, _, _) => ""
      case DeleteRequest(_, _, _, _, parameters, _, _) =>
        buildParameters(parameters, List.empty).mkString("(", ", ", ")")
    }
  }

  private def getVariableNameAndMethod(route: RouteDefinition): String =
    route match {
      case _: GetRequest                                           => "request @ GET"
      case RequestWithPayload(_, _, _, _, PUT, _, _, _, _, _, _)   => "request @ PUT"
      case RequestWithPayload(_, _, _, _, POST, _, _, _, _, _, _)  => "request @ POST"
      case RequestWithPayload(_, _, _, _, PATCH, _, _, _, _, _, _) => "request @ PATCH"
      case _: DeleteRequest                                        => "request @ DELETE"
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
    val queries = getQueryParams(route).toList
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
      case DeleteRequest(_, _, _, _, _, response, status)                  => buildString(response, status)
      case RequestWithPayload(_, _, _, _, _, _, _, _, response, _, status) => buildString(response, status)
      case GetRequest(_, _, _, _, _, _, response, status)                  => buildString(response, status)
    }
}
