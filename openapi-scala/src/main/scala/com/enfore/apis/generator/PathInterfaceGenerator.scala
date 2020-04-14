package com.enfore.apis.generator

import cats.data.NonEmptyList
import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.generator.ShowTypeTag.typeReprShowType
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr._

object PathInterfaceGenerator {
  import com.enfore.apis.generator.Utilities._

  def apply(implicit p: PackageName): ScalaGenerator[PathItemAggregation] =
    (agg: PathItemAggregation) => {
      val path: String          = agg.path
      val generatedCode: String = agg.items.map(routeDefGen.generateScala).mkString("\n\t")
      s"""
         |package ${p.name}.routes
         |
       |import com.enfore.apis.lib._
         |
       |object ${cleanScalaSymbol(path)} {
         |\t$generatedCode
         |}
     """.stripMargin.trim()
    }

  private def resolveRef(ref: Ref)(implicit p: PackageName) = s"${p.name}.${ref.typeName}"

  private def encodingFnDec(encodingNameReturnMap: Map[String, Ref], params: String)(implicit p: PackageName): String =
    encodingNameReturnMap
      .map {
        case (encodingName, typeRef) =>
          val returnType = resolveRef(typeRef)(p)
          s"def $encodingName($params): F[$returnType]"
      }
      .mkString("\n")

  private def queryListMaker(queries: Map[String, TypeRepr]): List[String] =
    queries.map {
      case (name: String, primType: TypeRepr) =>
        primType match {
          case PrimitiveInt(_)    => s""""$name" -> "int""""
          case PrimitiveNumber(_) => s""""$name" -> "double""""
          case PrimitiveString(_) => s""""$name" -> "string""""
          case _                  => s""""$name" -> "string""""
        }
    }.toList

  private def paramsMaker(queries: Map[String, TypeRepr], pathParams: List[String], reqType: Option[TypeRepr])(
      implicit p: PackageName
  ): String = {
    val querySyntax: Option[String] = NonEmptyList
      .fromList(queries.map {
        case (name, prim) => s"$name:  ${typeReprShowType.showType(prim)}"
      }.toList)
      .map(_.toList.mkString(", "))
    val pathParamsSyntax: Option[String] =
      NonEmptyList.fromList(pathParams.map(param => s"$param: String")).map(_.toList.mkString(", "))
    val reqSyntax: Option[String] = reqType.map {
      case request @ Ref(_, _) => s"request: ${resolveRef(request)(p)}"
      case x                   => s"request: ${x.typeName}"
    }
    List(querySyntax, pathParamsSyntax, reqSyntax).flatten
      .mkString(", ")
  }

  private def typelessParamsMaker(
      queries: Map[String, TypeRepr],
      pathParams: List[String],
      request: Boolean = false
  ): String =
    NonEmptyList
      .fromList(queries.keys.toList ++ pathParams)
      .map(_.toList)
      .map(list => if (request) list ++ List("request") else list)
      .fold(if (request) "request" else "")(_.mkString(", "))

  private def encodingCallMapper(encodingList: List[String], typelessParams: String): String =
    encodingList
      .map(
        encoding =>
          s"""case "$encoding" => F.map(${cleanScalaSymbol(encoding)}($typelessParams))(Coproduct[Response](_))"""
      )
      .mkString("\t\n")

  private def makeEncodingCallString(encodings: List[String], typelessParams: String): String =
    s"""
       |\t\t\t${encodingCallMapper(encodings, typelessParams)}
    """.stripMargin + "\t" +
      """case _ => ME.raiseError(EncodingMatchFailure(s"$encoding is not acceptable for $path"))""".stripMargin

  private def implGenerator(encodingList: Option[List[String]], params: String, typelessParams: String) =
    encodingList
      .flatMap(NonEmptyList.fromList(_).map(_.toList))
      .fold(s"""def impl($params): F[Response]""") { encodings =>
        s"""
           |\t\tdef impl(encoding: String)($params)(implicit ME: cats.MonadError[F, Throwable], F: cats.Functor[F]): F[Response] = encoding match {
           |  ${makeEncodingCallString(encodings, typelessParams)}
           |\t\t}
       """.stripMargin
      }

  private def postRequestGenerator(
      path: String,
      reqType: ReqWithContentType,
      pathParams: List[PathParameter],
      queries: Map[String, TypeRepr],
      request: Option[TypeRepr],
      response: Option[Map[String, Ref]]
  )(implicit p: PackageName): String = {
    val cleanEncodingReferences: Option[Map[String, Ref]] = response.map(_.map {
      case (k, v) => cleanScalaSymbol(k) -> v
    })
    val encodings: Option[Iterable[String]] = response.map(_.keys)
    val queryMap: List[String]              = queryListMaker(queries)
    val querySyntax: String                 = queryMap.mkString("Map(", ", ", ")")
    val pathVars: List[String]              = pathParams map (_.name) map cleanScalaSymbol
    val params: String                      = paramsMaker(queries, pathVars, request)(p)
    val typelessParams: String              = typelessParamsMaker(queries, pathVars, true)
    val reqName: String = reqType match {
      case ReqWithContentType.POST  => "Post"
      case ReqWithContentType.PUT   => "Put"
      case ReqWithContentType.PATCH => "Patch"
    }
    val responseType: String =
      response.map(r => MiniTypeHelpers.referenceCoproduct(r.values.toList)(p)).getOrElse("Unit")
    val shapelessImport: String = if (responseType == "Unit") "" else "import shapeless._\n\t\t"
    s"""
       |\ttrait $reqName[F[_]] extends ${reqName}Request {
       |\t\tval path = "$path"
       |\t\tval queries = $querySyntax
       |\t\tval pathVariables = ${pathVars.map(pv => s""" "$pv" """.trim).mkString("List(", ", ", ")")}
       |\t
       |\t\t${shapelessImport}type Response = $responseType
       |\t\t${cleanEncodingReferences
         .map(e => "\t" + encodingFnDec(e, params)(p))
         .getOrElse("")}
       |\t\t${implGenerator(encodings.map(_.toList), params, typelessParams)}
       |\t}
       """.stripMargin.trim
  }

  private def getRequestGenerator(
      path: String,
      pathParams: List[PathParameter],
      queries: Map[String, TypeRepr],
      response: Option[Map[String, Ref]]
  )(implicit p: PackageName): String = {
    val cleanEncodingReferences: Option[Map[String, Ref]] = response.map(_.map {
      case (k, v) => cleanScalaSymbol(k) -> v
    })
    val encodings: Option[Iterable[String]] = response.map(_.keys)
    val queryMap: List[String]              = queryListMaker(queries)
    val querySyntax: String                 = queryMap.mkString("Map(", ", ", ")")
    val pathVars: List[String]              = pathParams map (_.name) map cleanScalaSymbol
    val params: String                      = paramsMaker(queries, pathVars, None)(p)
    val typelessParams: String              = typelessParamsMaker(queries, pathVars)
    val responseType: String =
      response.map(r => MiniTypeHelpers.referenceCoproduct(r.values.toList)(p)).getOrElse("Unit")
    val shapelessImport: String = if (responseType == "Unit") "" else "import shapeless._\n\t\t"
    s"""
       |\ttrait Get[F[_]] extends GetRequest {
       |\t\tval path = "$path"
       |\t\tval queries = $querySyntax
       |\t\tval pathVariables = ${pathVars.map(pv => s""" "$pv" """.trim).mkString("List(", ", ", ")")}
       |\t
       |\t\t${shapelessImport}type Response = $responseType
       |\t${cleanEncodingReferences
         .map(e => "\t" + encodingFnDec(e, params)(p))
         .getOrElse("")}
       |\t\t${implGenerator(encodings.map(_.toList), params, typelessParams)}
       |\t}
       """.stripMargin.trim
  }

  private def deleteRequestGenerator(path: String, pathParams: List[PathParameter], response: Option[Map[String, Ref]])(
      implicit p: PackageName
  ): String = {
    val cleanEncodingReferences: Option[Map[String, Ref]] = response.map(_.map {
      case (k, v) => cleanScalaSymbol(k) -> v
    })
    val encodings: Option[Iterable[String]] = response.map(_.keys)
    val pathVars: List[String]              = pathParams map (_.name) map cleanScalaSymbol
    val params: String                      = paramsMaker(Map.empty, pathVars, None)(p)
    val typelessParams: String              = typelessParamsMaker(Map.empty, pathVars)
    val responseType: String =
      response.map(r => MiniTypeHelpers.referenceCoproduct(r.values.toList)(p)).getOrElse("Unit")
    val shapelessImport: String = if (responseType == "Unit") "" else "import shapeless._\n\t\t"
    s"""
       |\ttrait Delete[F[_]] extends DeleteRequest {
       |\t\tval path = "$path"
       |\t\tval queries = Map.empty[String, String]
       |\t\tval pathVariables = ${pathVars.map(pv => s""" "$pv" """.trim).mkString("List(", ", ", ")")}
       |\t
       |\t\t${shapelessImport}type Response = $responseType
       |\t${cleanEncodingReferences
         .map(e => "\t" + encodingFnDec(e, params)(p))
         .getOrElse("")}
       |\t\t${implGenerator(encodings.map(_.toList), params, typelessParams)}
       |\t }
       """.stripMargin.trim
  }

  private[generator] def routeDefGen(implicit p: PackageName): ScalaGenerator[RouteDefinition] = {
    case GetRequest(path, _, _, _, pathParams, queries, response, _) =>
      getRequestGenerator(path, pathParams, queries, response)
    case RequestWithPayload(path, _, _, _, reqType, pathParams, queries, request, response, _, _) =>
      postRequestGenerator(path, reqType, pathParams, queries, request, response)
    case DeleteRequest(path, _, _, _, pathParams, response, _) =>
      deleteRequestGenerator(path, pathParams, response)
  }

}
