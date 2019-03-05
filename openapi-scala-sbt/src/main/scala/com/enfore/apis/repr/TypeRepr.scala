package com.enfore.apis.repr

import com.enfore.apis.generator.ShowTypeTag

sealed trait TypeRepr

object TypeRepr {

  final case class Ref(path: String, typeName: String) extends TypeRepr

  sealed trait Symbol {
    val valName: String
    val typeName: String
    val ref: Ref
  }

  sealed trait Primitive                               extends TypeRepr
  case object PrimitiveString                          extends Primitive
  case object PrimitiveNumber                          extends Primitive
  case object PrimitiveInt                             extends Primitive
  case object PrimitiveBoolean                         extends Primitive
  final case class PrimitiveArray(dataType: TypeRepr)  extends Primitive
  final case class PrimitiveOption(dataType: TypeRepr) extends Primitive

  sealed trait NewType extends TypeRepr {
    val packageName: String
    val typeName: String
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

  private def symbolRefResolution(incoming: List[NewTypeSymbol]): Map[Ref, Boolean] =
    incoming.foldLeft(Map[Ref, Boolean]()) {
      case (refTable: Map[Ref, Boolean], s @ NewTypeSymbol(_, data: PrimitiveProduct)) =>
        // TODO: When found a NewTypeSymbol as a value of a product, add that to resolution list
        data.values.foldLeft(refTable + (s.ref -> true))((refChart: Map[Ref, Boolean], symbol: Symbol) =>
          refChart + (symbol.ref               -> true))
      case (refTable: Map[Ref, Boolean], x) => refTable + (x.ref -> true)
    }

  // At the moment this function is not being used because the Scala compiler
  // does the reference resolution checks
  def validateReferences(in: List[NewTypeSymbol]): Unit = {
    val failedResolutions: Iterable[Ref] = symbolRefResolution(in).filter(!_._2).keys
    failedResolutions
      .map(ref => s"ERROR: Could not resolve reference to ${ref.path}:${ref.typeName}")
      .foreach(System.err.println)
    System.exit(1)
  }

}
