package com.enfore.apis.generator

import cats.data.NonEmptyList
import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.repr.TypeRepr._
import cats.implicits._

import scala.annotation.tailrec

trait ScalaGenerator[T] {
  def generateScala(in: T): String
}

object MiniTypeHelpers {

  private def resolveRef(ref: Ref)(implicit p: PackageName) = s"${p.name}.${ref.typeName}"

  def coproductTypes(in: List[String]): String =
    NonEmptyList.fromList(in).map(_.toList.mkString("", " :+: ", " :+: CNil")).getOrElse("Unit")

  def referenceCoproduct(in: List[Ref])(implicit p: PackageName): String =
    coproductTypes(in.map(resolveRef))

}

object Utilities {

  def hyphenAndUnderscoreToCamel(name: String): String =
    "[-_]([a-z\\d])".r.replaceAllIn(name, { m =>
      m.group(1).toUpperCase()
    })

  val illegalScalaSymbols =
    List(".", ",", "-", ":", "/", "|", "[", "]", "@", "#", "!", "(", ")", "*", "%", "*", "{", "}", "'", ";")

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

  def containsIllegal(in: String): Boolean =
    illegalScalaSymbols.foldLeft(false)((buf, value) => buf || in.contains(value))

  def illegalOrForbidden(str: String): Boolean = forbiddenSymbols.contains(str) || containsIllegal(str)

  def sanitize(str: String): String =
    if (illegalOrForbidden(str)) s"`$str`" else str

  def cleanScalaSymbol(in: String): String =
    sanitize(hyphenAndUnderscoreToCamel(in))

  def cleanScalaSymbolEnum(in: String): String =
    sanitize(in)

}

object ScalaGenerator {

  import Utilities._

  private def primitiveSymbolGenerator(symbol: PrimitiveSymbol): String =
    s"${symbol.valName} : ${SymbolAnnotationMaker.makeAnnotation(symbol)}".trim

  private def refSymbolGenerator(symbol: RefSymbol): String =
    s"${symbol.valName}: ${SymbolAnnotationMaker.makeAnnotation(symbol)}".trim

  @tailrec def primitiveRefinementExtractor(in: Primitive): Option[Primitive] = in match {
    case s @ PrimitiveString(refinements: Option[NonEmptyList[RefinedTags]], _) =>
      if (refinements.map(_.head).isDefined) Some(s) else None
    case a @ PrimitiveArray(_, refinements: Option[NonEmptyList[RefinedTags]]) =>
      if (refinements.map(_.head).isDefined) Some(a) else None
    case i @ PrimitiveInt(refinements: Option[NonEmptyList[RefinedTags]], _) =>
      if (refinements.map(_.head).isDefined) Some(i) else None
    case n @ PrimitiveNumber(refinements: Option[NonEmptyList[RefinedTags]], _) =>
      if (refinements.map(_.head).isDefined) Some(n) else None
    case PrimitiveOption(data: Primitive) =>
      primitiveRefinementExtractor(data)
    case _ => None
  }

  private def refinementExtractor(in: Symbol): Option[Primitive] = in match {
    case PrimitiveSymbol(_, data) => primitiveRefinementExtractor(data)
    case _                        => None
  }

  private def refinementTypeDef(in: List[Symbol]): Option[NonEmptyList[String]] = {
    val generateAnnotationType: Symbol => String =
      SymbolAnnotationMaker.refinedAnnotation(_)(generateForAlias = false)
    NonEmptyList.fromList(in.map(sym => s"type `${sym.valName}Refined` = ${generateAnnotationType(sym)}"))
  }

  private def refinementSymbolMaker(in: List[Symbol]): Option[NonEmptyList[String]] = {
    val helpers = in
      .flatMap(sym => refinementExtractor(sym).map(_ => sym))
      .map {
        case PrimitiveSymbol(vn, PrimitiveOption(typeRepr: Primitive)) =>
          PrimitiveSymbol(vn, typeRepr)
        case t @ _ => t
      }
      .map(
        sym =>
          s"val ${cleanScalaSymbol(sym.valName)} = new RefinedTypeOps[${SymbolAnnotationMaker
            .refinedAnnotation(sym)(generateForAlias = true)}, ${SymbolAnnotationMaker.refinementOnType(sym)}]"
      )
    NonEmptyList.fromList(helpers)
  }

  private def generateForPrimitiveEnum(packageName: String, typeName: String, content: Set[String]): String =
    s"""
       |package $packageName\n
       |import enumeratum._\n
       |sealed trait $typeName extends EnumEntry
       |object $typeName extends Enum[$typeName] with CirceEnum[$typeName] {
       |  val values = findValues
       |  ${content
         .map(item => s"""case object ${cleanScalaSymbolEnum(item)} extends $typeName""")
         .mkString("", "\n\t", "")}
       | }
       """.stripMargin.trim

  private def generateForPrimitiveProduct(
      packageName: String,
      typeName: String,
      values: List[Symbol],
      summary: Option[String],
      description: Option[String]
  ): String = {
    val summaryDocs: List[String] = summary
      .map(_.split("\n").toList.map(l => s" * $l"))
      .getOrElse(Nil)

    val descriptionDocs = description
      .map(_.split("\n").toList.map(l => s" * $l"))
      .getOrElse(Nil)

    val docs =
      if ((summaryDocs ++ descriptionDocs).isEmpty) ""
      else ((("/**" +: summaryDocs) ++ descriptionDocs) :+ "**/").mkString("\n\t")

    val refinements: Option[String] = refinementSymbolMaker(values)
      .map(_.toList.mkString("\n\nobject RefinementConstructors {\n\t", "\n\t\t", "\n\t}"))
    val refinementTypeDefs: Option[String] = refinementTypeDef(values).map(_.toList.mkString("\n", "\n\t", "\n"))
    val refinedCode                        = (refinements, refinementTypeDefs).mapN(_ + _)
    val cleanedValues = values
      .map(
        sym =>
          s"${cleanScalaSymbol(sym.valName)} : ${SymbolAnnotationMaker.refinedAnnotation(sym)(generateForAlias = true)}"
      )
    val refinementImports = refinements.map(_ => """
      |
      |import eu.timepit.refined._
      |import eu.timepit.refined.api._
      |import eu.timepit.refined.collection._
      |import eu.timepit.refined.numeric._
      |import shapeless._
      |import eu.timepit.refined.boolean._
      |import io.circe.refined._
      |
      """.stripMargin)
    s"""
       |package $packageName\n
       |import io.circe._
       |import io.circe.generic.extras.Configuration
       |import io.circe.generic.extras.semiauto._\n${refinementImports.getOrElse("")}
       |$docs
       |final case class $typeName${cleanedValues.mkString("(\n\t", ",\n\t", "\n)")} \n
       |object $typeName {
       |\timplicit val customConfig = Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
       |\timplicit val circeDecoder: Decoder[$typeName] = deriveDecoder[$typeName]
       |\timplicit val circeEncoder: Encoder[$typeName] = deriveEncoder[$typeName]${refinedCode
         .getOrElse("")}
       |}
       """.stripMargin.trim
  }

  private def generateUnionJsonConversions(discriminator: String)(members: Set[Ref], margin: String = "\t\t"): String =
    members
      .map(_.typeName)
      .map(
        member =>
          s"""implicit def case${member} = at[$member](_.asJson.dropNullValues.deepMerge(Json.obj("$discriminator" -> Json.fromString("$member"))))"""
      )
      .mkString(s"\n$margin")

  private def generateUnionJsonMatch(members: Set[Ref], parentType: String, margin: String = "\t\t\t\t"): String =
    members
      .map(_.typeName)
      .map(member => s"""case Right("$member") => c.value.as[$member].map(Coproduct[$parentType](_))""")
      .mkString(s"\n$margin")

  private def generateForPrimitiveUnion(
      packageName: String,
      typeName: String,
      unionMembers: Set[Ref],
      discriminator: String,
      summary: Option[String],
      description: Option[String]
  ): String = {
    val summaryDocs: List[String] = summary
      .map(_.split("\n").toList.map(l => s" * $l"))
      .getOrElse(Nil)

    val descriptionDocs = description
      .map(_.split("\n").toList.map(l => s" * $l"))
      .getOrElse(Nil)

    val docs =
      if ((summaryDocs ++ descriptionDocs).isEmpty) ""
      else ((("/**" +: summaryDocs) ++ descriptionDocs) :+ "**/").mkString("\n\t")

    s"""
       |package $packageName\n
       |import shapeless._
       |import io.circe._
       |import io.circe.syntax._
       |import $typeName._
       |
       |final case class $typeName(value: Union)
       |
       |$docs
       |object $typeName {
       |  type Union = ${unionMembers.map(_.typeName).mkString("", " :+: ", " :+: CNil")}
       |
       |  object jsonConversions extends Poly1 {
       |    ${generateUnionJsonConversions(discriminator)(unionMembers)}
       |  }
       |
       |  implicit val customEncoders: Encoder[$typeName] = new Encoder[$typeName] {
       |    def apply(a: $typeName): Json = {
       |      (a.value map jsonConversions).unify
       |    }
       |  }
       |
       |  implicit val customDecoder: Decoder[$typeName] = new Decoder[$typeName] {
       |    def apply(c: HCursor): Decoder.Result[$typeName] = {
       |      val output = c.downField("$discriminator").as[String] match {
       |        ${generateUnionJsonMatch(unionMembers, "Union")}
       |        case _ => Left(DecodingFailure.apply("Type information not available", List(CursorOp.DownField("$discriminator"))))
       |      }
       |      output.map($typeName.apply)
       |    }
       |  }
       |}
       |""".stripMargin
  }

  private def newTypeSymbolGenerator(symbol: NewTypeSymbol): String = symbol match {
    case NewTypeSymbol(_, PrimitiveEnum(packageName, typeName, content, _, _)) =>
      generateForPrimitiveEnum(packageName, typeName, content)
    case NewTypeSymbol(_, PrimitiveProduct(packageName, typeName, values, summary, description)) =>
      generateForPrimitiveProduct(packageName, typeName, values, summary, description)
    case NewTypeSymbol(_, PrimitiveUnion(packageName, typeName, unionMembers, discriminator, summary, description)) =>
      generateForPrimitiveUnion(packageName, typeName, unionMembers, discriminator, summary, description)
  }

  val codeGenerator: ScalaGenerator[Symbol] = {
    case sym: PrimitiveSymbol => primitiveSymbolGenerator(sym)
    case sym: NewTypeSymbol   => newTypeSymbolGenerator(sym)
    case sym: RefSymbol       => refSymbolGenerator(sym)
  }

}
