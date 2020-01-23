package com.enfore.apis.repr

import com.enfore.apis.generator.ShowTypeTag
import io.circe._
import cats.syntax.functor._

sealed trait TypeRepr {
  val typeName: String
}

object TypeRepr {

  final case class Ref(path: String, typeName: String) extends TypeRepr

  sealed trait PrimitiveValue
  final case class PrimitiveStringValue(value: String) extends PrimitiveValue {
    override def toString: String = s""""$value""""
  }
  final case class PrimitiveNumberValue(value: Double) extends PrimitiveValue {
    override def toString: String = value.toString
  }
  final case class PrimitiveIntValue(value: Int) extends PrimitiveValue {
    override def toString: String = value.toString
  }
  final case class PrimitiveBooleanValue(value: Boolean) extends PrimitiveValue {
    override def toString: String = value.toString
  }

  implicit val primitiveStringDecoder: Decoder[PrimitiveStringValue] = (c: HCursor) =>
    c.as[String].map(PrimitiveStringValue)
  implicit val primitiveNumberDecoder: Decoder[PrimitiveNumberValue] = (c: HCursor) =>
    c.as[Double].map(PrimitiveNumberValue)
  implicit val primitiveIntDecoder: Decoder[PrimitiveIntValue] = (c: HCursor) => c.as[Int].map(PrimitiveIntValue)
  implicit val primitiveBooleanDecoder: Decoder[PrimitiveBooleanValue] = (c: HCursor) =>
    c.as[Boolean].map(PrimitiveBooleanValue)
  implicit val PrimitiveDecoder: Decoder[PrimitiveValue] =
    List[Decoder[PrimitiveValue]](
      Decoder[PrimitiveStringValue].widen,
      Decoder[PrimitiveNumberValue].widen,
      Decoder[PrimitiveIntValue].widen,
      Decoder[PrimitiveBooleanValue].widen
    ).reduceLeft(_ or _)

  sealed trait Primitive extends TypeRepr
  final case class PrimitiveString(refinements: Option[List[RefinedTags]]) extends Primitive {
    override val typeName: String = "String"
  }
  final case class PrimitiveNumber(refinements: Option[List[RefinedTags]]) extends Primitive {
    override val typeName: String = "Double"
  }
  final case class PrimitiveInt(refinements: Option[List[RefinedTags]]) extends Primitive {
    override val typeName: String = "Int"
  }
  final case class PrimitiveBoolean(refinements: Option[List[RefinedTags]]) extends Primitive {
    override val typeName: String = "Boolean"
  }
  final case class PrimitiveArray(dataType: TypeRepr, refinements: Option[List[RefinedTags]]) extends Primitive {
    override val typeName: String = s"Array[${dataType.typeName}]"
  }
  final case class PrimitiveOption(dataType: TypeRepr, defaultValue: Option[PrimitiveValue]) extends Primitive {
    override val typeName: String = s"Option[${dataType.typeName}]"
  }
  final case class PrimitiveDict(dataType: TypeRepr, defaultValue: Option[List[RefinedTags]]) extends Primitive {
    override val typeName: String = s"Map[String, ${dataType.typeName}]"
  }

  sealed trait RefinedTags
  sealed trait CollectionRefinements      extends RefinedTags
  final case class MinLength(length: Int) extends CollectionRefinements
  final case class MaxLength(length: Int) extends CollectionRefinements
  sealed trait IntegerRefinements         extends RefinedTags
  final case class Minimum(i: Int)        extends IntegerRefinements
  final case class Maximum(i: Int)        extends IntegerRefinements

  sealed trait NewType extends TypeRepr {
    val packageName: String
    val typeName: String
    // User documentation that can either be a short summary or description
    val summary: Option[String]
    val description: Option[String]
  }

  sealed trait Symbol {
    val valName: String
    val typeName: String
    val ref: Ref
  }

  final case class PrimitiveEnum(
      packageName: String,
      typeName: String,
      values: Set[String],
      summary: Option[String],
      description: Option[String])
      extends NewType
  final case class PrimitiveProduct(
      packageName: String,
      typeName: String,
      values: List[Symbol],
      summary: Option[String],
      description: Option[String])
      extends NewType
  final case class PrimitiveUnion(
      packageName: String,
      typeName: String,
      values: Set[Ref],
      discriminator: String,
      summary: Option[String],
      description: Option[String])
      extends NewType

  final case class PrimitiveSymbol(valName: String, dataType: Primitive) extends Symbol {
    val typeName: String = ShowTypeTag.typeReprShowType.showType(dataType)
    val ref              = Ref("root", typeName)
  }

  final case class NewTypeSymbol(valName: String, data: NewType) extends Symbol {
    val path: String     = data.packageName
    val typeName: String = data.typeName
    val ref: Ref         = Ref(path, typeName)
  }

  final case class RefSymbol(valName: String, ref: Ref) extends Symbol {
    val typeName: String = ref.typeName
  }

}
