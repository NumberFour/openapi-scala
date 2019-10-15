package com.enfore.apis.generator

import cats.data.NonEmptyList
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr._

object SymbolAnnotationMaker {

  import ShowTypeTag.typeReprShowType

  def refinementTagGenerator[T](refinements: NonEmptyList[T]): String = {
    def refinementMatcher(in: T) = in match {
      case MinLength(length) => s"MinSize[W.`$length`.T]"
      case MaxLength(length) => s"MaxSize[W.`$length`.T]"
    }
    s"Refined AllOf[${refinements.map(refinementMatcher).toList.mkString("", " :: ", " :: HNil")}]"
  }

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
    case PrimitiveSymbol(_, PrimitiveOption(data: Primitive, Some(defaultValue))) =>
      s"${dataRefinementMatcher(data)} = ${defaultValue}"
    case PrimitiveSymbol(_, PrimitiveOption(data: Primitive, None)) => s"Option[${dataRefinementMatcher(data)}]"
    case PrimitiveSymbol(_, data: Primitive)                        => dataRefinementMatcher(data)
    case x @ _                                                      => makeAnnotation(x)
  }

  def refinementOnType(symbol: Symbol): String = symbol match {
    case x @ PrimitiveSymbol(_, PrimitiveOption(data: Primitive, _)) => makeAnnotation(x.copy(dataType = data))
    case x @ _                                                       => makeAnnotation(x)
  }

}
