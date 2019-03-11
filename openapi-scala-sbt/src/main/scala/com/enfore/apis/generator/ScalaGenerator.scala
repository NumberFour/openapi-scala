package com.enfore.apis.generator

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
    case PrimitiveString                 => "String"
    case PrimitiveNumber                 => "Double"
    case PrimitiveInt                    => "Int"
    case PrimitiveBoolean                => "Boolean"
    case PrimitiveArray(data: TypeRepr)  => s"List[${data.showType}]"
    case PrimitiveOption(data: TypeRepr) => s"Option[${data.showType}]"
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

  protected def annotation(refType: RefSymbol): String = s"${refType.ref.path}.${refType.ref.typeName}"

  def makeAnnotation(symbol: Symbol): String = symbol match {
    case primType: PrimitiveSymbol => typeReprShowType.showType(primType.dataType)
    case auxType: NewTypeSymbol    => typeReprShowType.showType(auxType.data)
    case refType: RefSymbol        => typeReprShowType.showType(refType.ref)
  }

}

object ScalaGenerator {

  private val scalaSymbols = List(
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
    "case"
  )

  val illegalScalaSymbols = List(".", ",", "-", ":")

  def underscoreToCamel(name: String) =
    "_([a-z\\d])".r.replaceAllIn(name, { m =>
      m.group(1).toUpperCase()
    })

  private def cleanScalaSymbol(in: String): String = {
    val containsIllegal = illegalScalaSymbols.foldLeft(false)((buf, value) => buf || in.contains(value))
    if (scalaSymbols.contains(in) || containsIllegal) underscoreToCamel(s"`$in`")
    else underscoreToCamel(in)
  }

  private def primitiveSymbolGenerator(symbol: PrimitiveSymbol): String =
    s"${symbol.valName} : ${SymbolAnnotationMaker.makeAnnotation(symbol)}".trim

  private def refSymbolGenerator(symbol: RefSymbol): String =
    s"${symbol.valName}: ${SymbolAnnotationMaker.makeAnnotation(symbol)}".trim

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
      s"""
         |package $packageName\n
         |import io.circe._
         |import io.circe.derivation._\n
         |final case class $typeName${values
           .map(sym => s"${cleanScalaSymbol(sym.valName)} : ${SymbolAnnotationMaker.makeAnnotation(sym)}")
           .mkString("(", ",\n\t", ")")} \n
         | object $typeName {
         |  implicit val circeDecoder: Decoder[$typeName] = deriveDecoder[$typeName](renaming.snakeCase)
         |  implicit val circeEncoder: Encoder[$typeName] = deriveEncoder[$typeName](renaming.snakeCase)
         | }
       """.stripMargin.trim
  }

  implicit val codeGenerator: ScalaGenerator[Symbol] = {
    case sym: PrimitiveSymbol => primitiveSymbolGenerator(sym)
    case sym: NewTypeSymbol   => newTypeSymbolGenerator(sym)
    case sym: RefSymbol       => refSymbolGenerator(sym)
  }

}
