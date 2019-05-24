package com.enfore.apis.repr

import com.enfore.apis.generator.ShowTypeTag

sealed trait TypeRepr

object TypeRepr {

  final case class Ref(path: String, typeName: String) extends TypeRepr

  sealed trait PrimitiveValue
  final case class PrimitiveStringValue(value: String)   extends PrimitiveValue
  final case class PrimitiveNumberValue(value: Double)   extends PrimitiveValue
  final case class PrimitiveIntValue(value: Int)         extends PrimitiveValue
  final case class PrimitiveBooleanValue(value: Boolean) extends PrimitiveValue

  sealed trait Primitive                                                                      extends TypeRepr
  final case class PrimitiveString(refinements: Option[List[RefinedTags]])                    extends Primitive
  final case class PrimitiveNumber(refinements: Option[List[RefinedTags]])                    extends Primitive
  final case class PrimitiveInt(refinements: Option[List[RefinedTags]])                       extends Primitive
  final case class PrimitiveBoolean(refinements: Option[List[RefinedTags]])                   extends Primitive
  final case class PrimitiveArray(dataType: TypeRepr, refinements: Option[List[RefinedTags]]) extends Primitive
  final case class PrimitiveOption(dataType: TypeRepr, defaultValue: Option[PrimitiveValue])  extends Primitive

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
  }

  sealed trait Symbol {
    val valName: String
    val typeName: String
    val ref: Ref
  }

  final case class PrimitiveEnum(packageName: String, typeName: String, values: Set[String])     extends NewType
  final case class PrimitiveProduct(packageName: String, typeName: String, values: List[Symbol]) extends NewType

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
