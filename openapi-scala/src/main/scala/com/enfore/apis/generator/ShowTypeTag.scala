package com.enfore.apis.generator

import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr._
import simulacrum._

@typeclass trait ShowTypeTag[T] {
  def showType(in: T): String
}

object ShowTypeTag {
  import ops._
  implicit val primitiveShowType: ShowTypeTag[Primitive] = {
    case PrimitiveString(_)                => "String"
    case PrimitiveNumber(_)                => "Double"
    case PrimitiveInt(_)                   => "Int"
    case PrimitiveBoolean(_)               => "Boolean"
    case PrimitiveArray(data: TypeRepr, _) => s"List[${data.showType}]"
    case PrimitiveOption(data: TypeRepr, default) =>
      default.fold(s"Option[${data.showType}]")(d => s"${data.showType} = ${d.toString}")
    case PrimitiveDict(data: TypeRepr, _) => s"Map[String, ${data.showType}]"
  }

  implicit private val newTypeShowType: ShowTypeTag[NewType] = {
    case PrimitiveEnum(packageName: String, name: String, _)     => s"$packageName.$name.Values"
    case PrimitiveProduct(packageName: String, name: String, _)  => s"$packageName.$name"
    case PrimitiveUnion(packageName: String, name: String, _, _) => s"$packageName.${name}Union.$name"
  }

  implicit private val refTypeShowType: ShowTypeTag[Ref] = ref => s"${ref.path}.${ref.typeName}"

  implicit val typeReprShowType: ShowTypeTag[TypeRepr] = {
    case p: Primitive => p.showType
    case n: NewType   => n.showType
    case r: Ref       => r.showType
  }

}
