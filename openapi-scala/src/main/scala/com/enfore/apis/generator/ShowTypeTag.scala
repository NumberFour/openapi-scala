package com.enfore.apis.generator

import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr._

trait ShowTypeTag[T] {
  def showType(in: T): String
}

object ShowTypeTag {
  implicit val primitiveShowType: ShowTypeTag[Primitive] = {
    case _: PrimitiveString                => "String"
    case _: PrimitiveNumber                => "Double"
    case _: PrimitiveInt                   => "Int"
    case _: PrimitiveBoolean               => "Boolean"
    case PrimitiveArray(data: TypeRepr, _) => s"List[${typeReprShowType.showType(data)}]"
    case PrimitiveOption(data: TypeRepr)   => s"Option[${typeReprShowType.showType(data)}]"
    case PrimitiveDict(data: TypeRepr, _)  => s"Map[String, ${typeReprShowType.showType(data)}]"
  }

  implicit private val newTypeShowType: ShowTypeTag[NewType] = {
    case PrimitiveEnum(packageName: String, name: String, _, _, _)     => s"$packageName.$name.Values"
    case PrimitiveProduct(packageName: String, name: String, _, _, _)  => s"$packageName.$name"
    case PrimitiveUnion(packageName: String, name: String, _, _, _, _) => s"$packageName.${name}Union.$name"
  }

  implicit private val refTypeShowType: ShowTypeTag[Ref] = ref => s"${ref.path}.${ref.typeName}"

  implicit val typeReprShowType: ShowTypeTag[TypeRepr] = {
    case p: Primitive => primitiveShowType.showType(p)
    case n: NewType   => newTypeShowType.showType(n)
    case r: Ref       => refTypeShowType.showType(r)
  }

}
