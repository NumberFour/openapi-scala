package com.enfore.apis.generator

import cats.data.NonEmptyList
import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.repr.TypeRepr._
import simulacrum._

import scala.annotation.tailrec

@typeclass trait ScalaGenerator[T] {
  def generateScala(in: T): String
}
object MiniTypeHelpers {

  private def resolveRef(ref: Ref)(implicit p: PackageName) = s"${p.name}.${ref.typeName}"

  def coproductTypes(in: List[String]): String =
    NonEmptyList.fromList(in).map(_.toList.mkString("", " :+: ", " :+: CNil")).getOrElse("Unit")

  def referenceCoproduct(in: List[Ref])(implicit p: PackageName) =
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

  def underscoreToCamel(name: String): String =
    // Yes, this was copied from StackOverflow
    "_([a-z\\d])".r.replaceAllIn(name, { m =>
      m.group(1).toUpperCase()
    })

  def containsIllegal(in: String): Boolean =
    illegalScalaSymbols.foldLeft(false)((buf, value) => buf || in.contains(value))

  def cleanScalaSymbol(in: String): String =
    if (forbiddenSymbols.contains(in) || containsIllegal(in)) {
      val newVal = hyphenAndUnderscoreToCamel(s"$in")
      if (forbiddenSymbols.contains(newVal) || containsIllegal(newVal)) s"`$newVal`" else newVal
    } else hyphenAndUnderscoreToCamel(in)

}

object ScalaGenerator {

  import Utilities._

  private def primitiveSymbolGenerator(symbol: PrimitiveSymbol): String =
    s"${symbol.valName} : ${SymbolAnnotationMaker.makeAnnotation(symbol)}".trim

  private def refSymbolGenerator(symbol: RefSymbol): String =
    s"${symbol.valName}: ${SymbolAnnotationMaker.makeAnnotation(symbol)}".trim

  @tailrec private def refinementExtractor(in: Symbol): Option[Primitive] = in match {
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
         |\timplicit val circeDecoder: Decoder[$typeName] = deriveDecoder[$typeName](renaming.snakeCase, true, None)
         |\timplicit val circeEncoder: Encoder[$typeName] = deriveEncoder[$typeName](renaming.snakeCase, None)${refinements
           .getOrElse("")}
         |}
       """.stripMargin.trim
  }

  implicit val codeGenerator: ScalaGenerator[Symbol] = {
    case sym: PrimitiveSymbol => primitiveSymbolGenerator(sym)
    case sym: NewTypeSymbol   => newTypeSymbolGenerator(sym)
    case sym: RefSymbol       => refSymbolGenerator(sym)
  }

}
