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
      case Minimum(size)     => s"GreaterEqual[W.`$size`.T]"
      case Maximum(size)     => s"LessEqual[W.`$size`.T]"
    }
    s"Refined AllOf[${refinements.map(refinementMatcher).toList.mkString("", " :: ", " :: HNil")}]"
  }

  def makeAnnotation(symbol: Symbol): String = symbol match {
    case primType: PrimitiveSymbol => typeReprShowType.showType(primType.dataType)
    case auxType: NewTypeSymbol    => typeReprShowType.showType(auxType.data)
    case refType: RefSymbol        => typeReprShowType.showType(refType.ref)
  }

  /**
    * Generates a type-tag including refinements for a given primitive type.
    * For example, a String would have {{{ String Refined AllOf[...] }}}
    * @param in Primitive type
    * @return Type signature as a string (including refinements)
    */
  def primitiveTypeSigWithRefinements(in: TypeRepr): String = in match {
    case PrimitiveString(refinements) =>
      val base = "String"
      refinements
        .fold(base)(r => s"$base ${refinementTagGenerator(r)}")
    case PrimitiveArray(data: TypeRepr, refinements) =>
      val base = s"List[${ShowTypeTag.typeReprShowType.showType(data)}]"
      refinements
        .fold(base)(r => s"$base ${refinementTagGenerator(r)}")
    case PrimitiveInt(refinements) =>
      val base = "Int"
      refinements
        .fold(base)(r => s"$base ${refinementTagGenerator(r)}")
    case PrimitiveNumber(refinements) =>
      val base = "Double"
      refinements
        .fold(base)(r => s"$base ${refinementTagGenerator(r)}")
    case PrimitiveOption(x: Primitive, _) => s"Option[${this.primitiveTypeSigWithRefinements(x)}]"
    case x @ _                            => ShowTypeTag.typeReprShowType.showType(x)
  }

  /**
    * This function lets you resolve the type signature of a primitive with refinement
    * if it is a primitive type. For all other types of representations, it ignores and
    * shows a regular type tag without taking into account any possible nested refinements
    */
  def onlyResolverTopPrimitiveRefinements(in: TypeRepr): String = in match {
    case p: Primitive => primitiveTypeSigWithRefinements(p)
    case n: NewType   => ShowTypeTag.typeReprShowType.showType(n)
    case r: Ref       => ShowTypeTag.typeReprShowType.showType(r)
  }

  /**
    * Generate the type after applying the refinements to the type derived from the symbol
    * provided as input.
    *
    * @param symbol           Symbol to derive the type to be refined
    * @param generateForAlias Flag that tells weather the code is being generated for type annotation
    *                         or type alias definition
    */
  def refinedAnnotation(symbol: Symbol)(generateForAlias: Boolean = true): String = symbol match {
    case PrimitiveSymbol(_, PrimitiveOption(data: Primitive, Some(defaultValue))) =>
      val possibleDefault = if (generateForAlias) s" = $defaultValue" else ""
      s"${primitiveTypeSigWithRefinements(data)}$possibleDefault"
    case PrimitiveSymbol(_, PrimitiveOption(data: Primitive, None)) =>
      val content = primitiveTypeSigWithRefinements(data)
      if (generateForAlias) s"Option[$content]" else content
    case PrimitiveSymbol(_, data: Primitive) => primitiveTypeSigWithRefinements(data)
    case x @ _                               => makeAnnotation(x)
  }

  def refinementOnType(symbol: Symbol): String = symbol match {
    case x @ PrimitiveSymbol(_, PrimitiveOption(data: Primitive, _)) => makeAnnotation(x.copy(dataType = data))
    case x @ _                                                       => makeAnnotation(x)
  }

}
