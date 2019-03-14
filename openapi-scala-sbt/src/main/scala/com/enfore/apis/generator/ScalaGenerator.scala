package com.enfore.apis.generator

import cats.data.NonEmptyList
import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr._
import simulacrum._

@typeclass trait ScalaGenerator[T] {
  def generateScala(in: T): String
}

@typeclass trait ShowTypeTag[T] {
  def showType(in: T): String
}

object ShowTypeTag {
  import ops._

  implicit private val primitiveShowType: ShowTypeTag[Primitive] = {
    case PrimitiveString(_)                => "String"
    case PrimitiveNumber(_)                => "Double"
    case PrimitiveInt(_)                   => "Int"
    case PrimitiveBoolean(_)               => "Boolean"
    case PrimitiveArray(data: TypeRepr, _) => s"List[${data.showType}]"
    case PrimitiveOption(data: TypeRepr)   => s"Option[${data.showType}]"
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

  protected def annotation(refType: RefSymbol): String = s"${refType.ref.path}.${refType.ref.typeName}"

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
    case PrimitiveSymbol(_, PrimitiveOption(data: Primitive)) => s"Option[${dataRefinementMatcher(data)}]"
    case PrimitiveSymbol(_, data: Primitive)                  => dataRefinementMatcher(data)
    case x @ _                                                => this.makeAnnotation(x)
  }

  def refinementOnType(symbol: Symbol): String = symbol match {
    case x @ PrimitiveSymbol(_, PrimitiveOption(data: Primitive)) => this.makeAnnotation(x.copy(dataType = data))
    case x @ _                                                    => this.makeAnnotation(x)
  }

}

object ScalaGenerator {

  // List of symbol names that are not allowed to be used directly
  private val forbiddenSymbols = List(
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

  val illegalScalaSymbols = List(".", ",", "-", ":")

  def underscoreToCamel(name: String): String =
    "_([a-z\\d])".r.replaceAllIn(name, { m =>
      m.group(1).toUpperCase()
    })

  private def cleanScalaSymbol(in: String): String = {
    val containsIllegal = illegalScalaSymbols.foldLeft(false)((buf, value) => buf || in.contains(value))
    if (forbiddenSymbols.contains(in) || containsIllegal) underscoreToCamel(s"`$in`")
    else underscoreToCamel(in)
  }

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
        case PrimitiveOption(data: Primitive) =>
          refinementExtractor(PrimitiveSymbol(in.valName, data))
        case _ => None // TODO: Enable more refinements here
      }
    case _ => None
  }

  private def refinementSymbolMaker(in: List[Symbol]): Option[NonEmptyList[String]] = {
    val helpers = in
      .flatMap(sym => refinementExtractor(sym).map(_ => sym))
      .map {
        case PrimitiveSymbol(vn, PrimitiveOption(typeRepr: Primitive)) =>
          PrimitiveSymbol(vn, typeRepr)
        case t @ _ => t
      }
      .map(sym =>
        s"val ${cleanScalaSymbol(sym.valName)} = new RefinedTypeOps[${SymbolAnnotationMaker
          .refinedAnnotation(sym)}, ${SymbolAnnotationMaker.refinementOnType(sym)}]")
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
           .mkString("", "\n", "")}
         | }
       """.stripMargin
    case NewTypeSymbol(_, PrimitiveProduct(packageName, typeName, values)) =>
      assert(values.nonEmpty, s"$packageName.$typeName contains 0 values. This is not allowed.")
      val refinements: Option[String] = refinementSymbolMaker(values)
        .map(_.toList.mkString("object RefinementConstructors {\n\t", "\n\t\t", "\n\t}"))
      val refinementImports = refinements.map(_ => """
          |import eu.timepit.refined._
          |import eu.timepit.refined.api._
          |import eu.timepit.refined.collection._
          |import shapeless._
          |import eu.timepit.refined.boolean._
          |import io.circe.refined._
        """.stripMargin)
      s"""
         |package $packageName\n
         |import io.circe._
         |import io.circe.derivation._\n
         |${refinementImports.getOrElse("")}\n
         |final case class $typeName${values
           .map(sym => s"${cleanScalaSymbol(sym.valName)} : ${SymbolAnnotationMaker.refinedAnnotation(sym)}")
           .mkString("(", ",\n\t", ")")} \n
         | object $typeName {
         |  implicit val circeDecoder: Decoder[$typeName] = deriveDecoder[$typeName](renaming.snakeCase)
         |  implicit val circeEncoder: Encoder[$typeName] = deriveEncoder[$typeName](renaming.snakeCase)
         |
         |  ${refinements.getOrElse("")}
         | }
       """.stripMargin.trim
  }

  implicit val codeGenerator: ScalaGenerator[Symbol] = {
    case sym: PrimitiveSymbol => primitiveSymbolGenerator(sym)
    case sym: NewTypeSymbol   => newTypeSymbolGenerator(sym)
    case sym: RefSymbol       => refSymbolGenerator(sym)
  }

}
