package com.enfore.apis.generator

import cats.data.NonEmptyList
import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr._
import simulacrum._

@typeclass trait ScalaGenerator[T] {
  def generateScala(in: T): String
}

@typeclass trait ShowTypeTag[T] {
  def showType(in: T): String
}

object ShowTypeTag {
  import ops._

  implicit val primitiveShowType: ShowTypeTag[Primitive] = {
    case PrimitiveString(_)                 => "String"
    case PrimitiveNumber(_)                 => "Double"
    case PrimitiveInt(_)                    => "Int"
    case PrimitiveBoolean(_)                => "Boolean"
    case PrimitiveArray(data: TypeRepr, _)  => s"List[${data.showType}]"
    case PrimitiveOption(data: TypeRepr, _) => s"Option[${data.showType}]"
  }

  implicit private val newTypeShowType: ShowTypeTag[NewType] = {
    case PrimitiveEnum(packageName: String, name: String, _)    => s"$packageName.$name.Values"
    case PrimitiveProduct(packageName: String, name: String, _) => s"$packageName.$name"
  }

  implicit private val refTypeShowType: ShowTypeTag[Ref] = ref => s"${ref.path}.${ref.typeName}"

  implicit val typeReprShowType: ShowTypeTag[TypeRepr] = {
    case p: Primitive => p.showType
    case n: NewType   => n.showType
    case r: Ref       => r.showType
  }

}

object SymbolAnnotationMaker {

  import ShowTypeTag.typeReprShowType

  def refinementTagGenerator[T](refinements: NonEmptyList[T]): String = {
    def refinementMatcher(in: T) = in match {
      case MinLength(length) => s"MinSize[W.`$length`.T]"
      case MaxLength(length) => s"MaxSize[W.`$length`.T]"
    }
    s"Refined AllOf[${refinements.map(refinementMatcher).toList.mkString("", " :: ", " :: HNil")}]"
  }

  def makeAnnotation(symbol: Symbol): String = symbol match {
    case primType: PrimitiveSymbol => typeReprShowType.showType(primType.dataType)
    case auxType: NewTypeSymbol    => typeReprShowType.showType(auxType.data)
    case refType: RefSymbol        => typeReprShowType.showType(refType.ref)
  }

  def dataRefinementMatcher(in: Primitive): String = in match {
    case PrimitiveString(refinements) =>
      val base = "String"
      refinements
        .flatMap(NonEmptyList.fromList)
        .fold(base)(r => s"$base ${refinementTagGenerator(r)}")
    case PrimitiveArray(data: TypeRepr, refinements) =>
      val base = s"List[${ShowTypeTag.typeReprShowType.showType(data)}]"
      refinements
        .flatMap(NonEmptyList.fromList)
        .fold(base)(r => s"$base ${refinementTagGenerator(r)}")
    case x @ _ => ShowTypeTag.typeReprShowType.showType(x)
  }

  def refinedAnnotation(symbol: Symbol): String = symbol match {
    case PrimitiveSymbol(_, PrimitiveOption(data: Primitive, _)) => s"Option[${dataRefinementMatcher(data)}]"
    case PrimitiveSymbol(_, data: Primitive)                     => dataRefinementMatcher(data)
    case x @ _                                                   => makeAnnotation(x)
  }

  def refinementOnType(symbol: Symbol): String = symbol match {
    case x @ PrimitiveSymbol(_, PrimitiveOption(data: Primitive, _)) => makeAnnotation(x.copy(dataType = data))
    case x @ _                                                       => makeAnnotation(x)
  }

}

object MiniTypeHelpers {

  private def resolveRef(ref: Ref)(implicit p: PackageName) = s"${p.name}.${ref.typeName}"

  def coproductTypes(in: List[String]): String =
    NonEmptyList.fromList(in).map(_.toList.mkString("", " :+: ", " :+: CNil")).getOrElse("Unit")

  def referenceCoproduct(in: List[Ref])(implicit p: PackageName) =
    coproductTypes(in.map(resolveRef))

}

object ScalaGenerator {

  import ShowTypeTag.typeReprShowType

  // List of symbol names that are not allowed to be used directly
  val forbiddenSymbols = List(
    "type",
    "class",
    "trait",
    "object",
    "for",
    "Some",
    "None",
    "yield",
    "match",
    "private",
    "val",
    "final",
    "implicit",
    "protected",
    "public",
    "extends",
    "var",
    "def",
    "case",
    "Refined"
  )

  val illegalScalaSymbols =
    List(".", ",", "-", ":", "/", "|", "[", "]", "@", "#", "!", "(", ")", "*", "%", "*", "{", "}", "'", ";")

  def underscoreToCamel(name: String): String =
    // Yes, this was copied from StackOverflow
    "_([a-z\\d])".r.replaceAllIn(name, { m =>
      m.group(1).toUpperCase()
    })

  def hyphenAndUnderscoreToCamel(name: String): String =
    "[-_]([a-z\\d])".r.replaceAllIn(name, { m =>
      m.group(1).toUpperCase()
    })

  def cleanScalaSymbol(in: String): String = {
    val containsIllegal = illegalScalaSymbols.foldLeft(false)((buf, value) => buf || in.contains(value))
    if (forbiddenSymbols.contains(in) || containsIllegal) underscoreToCamel(s"`$in`")
    else underscoreToCamel(in)
  }

  private def resolveRef(ref: Ref)(implicit p: PackageName) = s"${p.name}.${ref.typeName}"

  private def primitiveSymbolGenerator(symbol: PrimitiveSymbol): String =
    s"${symbol.valName} : ${SymbolAnnotationMaker.makeAnnotation(symbol)}".trim

  private def refSymbolGenerator(symbol: RefSymbol): String =
    s"${symbol.valName}: ${SymbolAnnotationMaker.makeAnnotation(symbol)}".trim

  private def refinementExtractor(in: Symbol): Option[Primitive] = in match {
    case PrimitiveSymbol(_, data) =>
      data match {
        case s @ PrimitiveString(refinements: Option[List[RefinedTags]]) =>
          if (refinements.flatMap(_.headOption).isDefined) Some(s) else None
        case a @ PrimitiveArray(_, refinements: Option[List[RefinedTags]]) =>
          if (refinements.flatMap(_.headOption).isDefined) Some(a) else None
        case PrimitiveOption(data: Primitive, _) =>
          refinementExtractor(PrimitiveSymbol(in.valName, data))
        case _ => None // TODO: META-6086 Enable more refinements here
      }
    case _ => None
  }

  private def refinementSymbolMaker(in: List[Symbol]): Option[NonEmptyList[String]] = {
    val helpers = in
      .flatMap(sym => refinementExtractor(sym).map(_ => sym))
      .map {
        case PrimitiveSymbol(vn, PrimitiveOption(typeRepr: Primitive, _)) =>
          PrimitiveSymbol(vn, typeRepr)
        case t @ _ => t
      }
      .map(
        sym =>
          s"val ${cleanScalaSymbol(sym.valName)} = new RefinedTypeOps[${SymbolAnnotationMaker
            .refinedAnnotation(sym)}, ${SymbolAnnotationMaker.refinementOnType(sym)}]"
      )
    NonEmptyList.fromList(helpers)
  }

  private def newTypeSymbolGenerator(symbol: NewTypeSymbol): String = symbol match {
    case NewTypeSymbol(_, PrimitiveEnum(packageName, typeName, content)) =>
      s"""
         |package $packageName\n
         |import enumeratum._\n
         |sealed trait $typeName extends EnumEntry
         |object $typeName extends Enum[$typeName] with CirceEnum[$typeName] {
         |  val values = findValues
         |  ${content
           .map(item => s"""case object ${cleanScalaSymbol(item)} extends $typeName""")
           .mkString("", "\n\t", "")}
         | }
       """.stripMargin.trim
    case NewTypeSymbol(_, PrimitiveProduct(packageName, typeName, values)) =>
      assert(values.nonEmpty, s"$packageName.$typeName contains 0 values. This is not allowed.")
      val refinements: Option[String] = refinementSymbolMaker(values)
        .map(_.toList.mkString("\n\nobject RefinementConstructors {\n\t", "\n\t\t", "\n\t}"))
      val refinementImports = refinements.map(_ => """
          |
          |import eu.timepit.refined._
          |import eu.timepit.refined.api._
          |import eu.timepit.refined.collection._
          |import shapeless._
          |import eu.timepit.refined.boolean._
          |import io.circe.refined._
          |
          """.stripMargin)
      s"""
         |package $packageName\n
         |import io.circe._
         |import io.circe.derivation._\n${refinementImports.getOrElse("")}
         |final case class $typeName${values
           .map(sym => s"${cleanScalaSymbol(sym.valName)} : ${SymbolAnnotationMaker.refinedAnnotation(sym)}")
           .mkString("(\n\t", ",\n\t", "\n)")} \n
         |object $typeName {
         |\timplicit val circeDecoder: Decoder[$typeName] = deriveDecoder[$typeName](renaming.snakeCase)
         |\timplicit val circeEncoder: Encoder[$typeName] = deriveEncoder[$typeName](renaming.snakeCase)${refinements
           .getOrElse("")}
         |}
       """.stripMargin.trim
  }

  implicit val codeGenerator: ScalaGenerator[Symbol] = {
    case sym: PrimitiveSymbol => primitiveSymbolGenerator(sym)
    case sym: NewTypeSymbol   => newTypeSymbolGenerator(sym)
    case sym: RefSymbol       => refSymbolGenerator(sym)
  }

  private def encodingFnDec(encodingNameReturnMap: Map[String, Ref], params: String)(implicit p: PackageName): String =
    encodingNameReturnMap
      .map {
        case (encodingName, typeRef) =>
          val returnType = resolveRef(typeRef)(p)
          s"def $encodingName($params): F[$returnType]"
      }
      .mkString("\n")

  private def queryListMaker(queries: Map[String, Primitive]): List[String] =
    queries.map {
      case (name: String, primType: Primitive) =>
        primType match {
          case PrimitiveInt(_)    => s""""$name" -> "int""""
          case PrimitiveNumber(_) => s""""$name" -> "double""""
          case PrimitiveString(_) => s""""$name" -> "string""""
          case _                  => s""""$name" -> "string""""
        }
    }.toList

  private def paramsMaker(queries: Map[String, Primitive], pathParams: List[String], reqType: Option[Ref])(
      implicit p: PackageName
  ): String = {
    val querySyntax: Option[String] = NonEmptyList
      .fromList(queries.map {
        case (name, prim) => s"$name:  ${typeReprShowType.showType(prim)}"
      }.toList)
      .map(_.toList.mkString(", "))
    val pathParamsSyntax: Option[String] =
      NonEmptyList.fromList(pathParams.map(param => s"$param: String")).map(_.toList.mkString(", "))
    val reqSyntax: Option[String] = reqType.map(request => s"request: ${resolveRef(request)(p)}")
    List(querySyntax, pathParamsSyntax, reqSyntax).flatten
      .mkString(", ")
  }

  private def typelessParamsMaker(
      queries: Map[String, Primitive],
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
      queries: Map[String, Primitive],
      request: Ref,
      response: Option[Map[String, Ref]]
  )(implicit p: PackageName): String = {
    val cleanEncodingReferences: Option[Map[String, Ref]] = response.map(_.map {
      case (k, v) => cleanScalaSymbol(k) -> v
    })
    val encodings: Option[Iterable[String]] = response.map(_.keys)
    val queryMap: List[String]              = queryListMaker(queries)
    val querySyntax: String                 = queryMap.mkString("Map(", ", ", ")")
    val pathVars: List[String]              = pathParams map (_.name) map cleanScalaSymbol
    val params: String                      = paramsMaker(queries, pathVars, Some(request))(p)
    val typelessParams: String              = typelessParamsMaker(queries, pathVars, true)
    val reqName: String = reqType match {
      case ReqWithContentType.POST => "Post"
      case ReqWithContentType.PUT  => "Put"
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
      queries: Map[String, Primitive],
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
    case GetRequest(path, pathParams, queries, response, _) =>
      getRequestGenerator(path, pathParams, queries, response)
    case PutOrPostRequest(path, reqType, pathParams, queries, request, response, _, _) =>
      postRequestGenerator(path, reqType, pathParams, queries, request, response)
    case DeleteRequest(path, pathParams, response, _) =>
      deleteRequestGenerator(path, pathParams, response)
  }

  def pathInterfaceGen(implicit p: PackageName): ScalaGenerator[PathItemAggregation] =
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

}
