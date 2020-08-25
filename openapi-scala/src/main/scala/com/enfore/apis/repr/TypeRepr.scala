package com.enfore.apis.repr

import cats.data.NonEmptyList
import com.enfore.apis.generator.ShowTypeTag
import io.circe._
import cats.syntax.functor._

sealed trait TypeRepr {
  val typeName: String
  def packDefault(value: TypeRepr.PrimitiveValue): TypeRepr
  def default: Option[_]

  /**
    * @return The string representation of `default`
    */
  def defaultString: Option[String] =
    throw new IllegalAccessError(
      s"$typeName with type ${this.getClass.getName} has no default value implementation. This should never have happened. Default case encountered is ${default.toString}"
    )
}

object TypeRepr {

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

  final case class Ref(
      path: String,
      typeName: String,
      default: Option[String],
      readOnly: Option[Boolean],
      writeOnly: Option[Boolean]
  ) extends TypeRepr {
    override def packDefault(value: PrimitiveValue): TypeRepr = value match {
      case PrimitiveStringValue(defaultValue) => this.copy(default = Some(defaultValue))
      case _                                  => throw new AssertionError(s"Could not use $value as default for Ref $typeName")
    }

    override def defaultString: Option[String] = default.map(value => s"$path.$typeName.$value")
  }

  sealed trait Primitive extends TypeRepr {
    def defaultString: Option[String]
  }

  final case class PrimitiveString(refinements: Option[NonEmptyList[RefinedTags]], default: Option[String])
      extends Primitive {
    override val typeName: String = "String"

    override def defaultString: Option[String] = default.map(value => s""""$value"""")

    override def packDefault(value: PrimitiveValue): TypeRepr = value match {
      case PrimitiveStringValue(defaultValue) => this.copy(default = Some(defaultValue))
      case _                                  => throw new AssertionError(s"Could not use $value as default value for String $typeName")
    }
  }
  final case class PrimitiveNumber(refinements: Option[NonEmptyList[RefinedTags]], default: Option[Double])
      extends Primitive {
    override val typeName: String = "Double"

    override def defaultString: Option[String] = default.map(_.toString)

    override def packDefault(value: PrimitiveValue): TypeRepr = value match {
      case PrimitiveNumberValue(defaultValue) => this.copy(default = Some(defaultValue))
      case _                                  => throw new AssertionError(s"Could not use $value as default for Number $typeName")
    }

  }
  final case class PrimitiveInt(refinements: Option[NonEmptyList[RefinedTags]], default: Option[Int])
      extends Primitive {
    override val typeName: String = "Int"

    override def defaultString: Option[String] = default.map(_.toString)

    override def packDefault(value: PrimitiveValue): TypeRepr = {
      val err = new AssertionError(s"Could not use $value as default for Integer $typeName")
      value match {
        case PrimitiveIntValue(defaultValue) => this.copy(default = Some(defaultValue))
        case PrimitiveNumberValue(defaultValue) =>
          if (defaultValue.isValidInt) this.copy(default = Some(defaultValue.toInt)) else throw err
        case _ => throw err
      }
    }

  }
  final case class PrimitiveBoolean(refinements: Option[NonEmptyList[RefinedTags]], default: Option[Boolean])
      extends Primitive {
    override val typeName: String = "Boolean"

    override def defaultString: Option[String] = default.map(_.toString)

    override def packDefault(value: PrimitiveValue): TypeRepr = value match {
      case PrimitiveBooleanValue(defaultValue) => this.copy(default = Some(defaultValue))
      case _                                   => throw new AssertionError(s"Could not use $value as default for Boolean $typeName")
    }

  }
  final case class PrimitiveArray(dataType: TypeRepr, refinements: Option[NonEmptyList[RefinedTags]])
      extends Primitive {
    override val typeName: String = s"Array[${dataType.typeName}]"

    override def defaultString: Option[String] = None

    override def packDefault(value: PrimitiveValue): TypeRepr =
      throw new AssertionError(
        s"Default mapping is not implemented for arrays of ${dataType.typeName}. We could not generate default value for $typeName from $value"
      )

    override def default: Option[_] = None
  }
  final case class PrimitiveOption(dataType: TypeRepr) extends Primitive {
    override val typeName: String = s"Option[${dataType.typeName}]"

    override def defaultString: Option[String] = None

    override def packDefault(value: PrimitiveValue): TypeRepr =
      throw new AssertionError(
        s"Default mapping is not implemented for options of ${dataType.typeName}. We could not generate default value for $typeName from $value"
      )

    override def default: Option[_] = None
  }
  final case class PrimitiveDict(dataType: TypeRepr, defaultValue: Option[NonEmptyList[RefinedTags]])
      extends Primitive {
    override val typeName: String = s"Map[String, ${dataType.typeName}]"

    override def defaultString: Option[String] = None

    override def packDefault(value: PrimitiveValue): TypeRepr =
      throw new AssertionError(
        s"Default mapping is not implemented for dictionaries of ${dataType.typeName}. We could not generate default value for $typeName from $value"
      )

    override def default: Option[_] = None
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
  }

  final case class PrimitiveSymbol(valName: String, dataType: Primitive) extends Symbol {
    val typeName: String = ShowTypeTag.typeReprShowType.showType(dataType)
  }

  final case class NewTypeSymbol(valName: String, data: NewType) extends Symbol {
    val path: String     = data.packageName
    val typeName: String = data.typeName
  }

  final case class RefSymbol(valName: String, ref: Ref) extends Symbol {
    val typeName: String = ref.typeName
  }

  final case class PrimitiveEnum(
      packageName: String,
      typeName: String,
      values: Set[String],
      summary: Option[String],
      description: Option[String]
  ) extends NewType {
    override def packDefault(value: PrimitiveValue): TypeRepr =
      throw new AssertionError(
        s"You are setting a default value in an ENUM. This should be done in a reference of the ENUM instead. Failing default value generation for $typeName with $value"
      )

    override def default: Option[_] = None
  }
  final case class PrimitiveProduct(
      packageName: String,
      typeName: String,
      values: List[Symbol],
      summary: Option[String],
      description: Option[String]
  ) extends NewType {
    override def packDefault(value: PrimitiveValue): TypeRepr =
      throw new AssertionError(
        s"Default values for product types are not supported. Rejecting default generation for $typeName with value $value"
      )

    override def default: Option[_] = None
  }
  final case class PrimitiveUnion(
      packageName: String,
      typeName: String,
      values: Set[Ref],
      discriminator: String,
      summary: Option[String],
      description: Option[String]
  ) extends NewType {
    override def packDefault(value: PrimitiveValue): TypeRepr =
      throw new AssertionError(
        s"Default values for complex unions are not supported. Rejecting default generation for $typeName with value $value"
      )

    override def default: Option[_] = None
  }

}
