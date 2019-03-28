package com.enfore.apis.repr

import com.enfore.apis.generator.ShowTypeTag

sealed trait TypeRepr

object TypeRepr {

  final case class Ref(path: String, typeName: String) extends TypeRepr

  // MARK: Types for types --------------------------------------------

  sealed trait Symbol {
    val valName: String
    val typeName: String
    val ref: Ref
  }

  sealed trait Primitive                                                                      extends TypeRepr
  final case class PrimitiveString(refinements: Option[List[RefinedTags]])                    extends Primitive
  final case class PrimitiveNumber(refinements: Option[List[RefinedTags]])                    extends Primitive
  final case class PrimitiveInt(refinements: Option[List[RefinedTags]])                       extends Primitive
  final case class PrimitiveBoolean(refinements: Option[List[RefinedTags]])                   extends Primitive
  final case class PrimitiveArray(dataType: TypeRepr, refinements: Option[List[RefinedTags]]) extends Primitive
  final case class PrimitiveOption(dataType: TypeRepr)                                        extends Primitive

  sealed trait RefinedTags
  sealed trait CollectionRefinements      extends RefinedTags
  final case class MinLength(length: Int) extends CollectionRefinements
  final case class MaxLength(length: Int) extends CollectionRefinements

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

  // MARK: Types for route definitions -------------------------------

  sealed trait RouteDefinition {
    val path: String
  }

  final case class PathItemAggregation(path: String, items: List[RouteDefinition])

  /**
    * This sum type is used to mark a request that will have content body and may
    * return a response.
    */
  sealed trait ReqWithContentType
  object ReqWithContentType {
    case object POST extends ReqWithContentType
    case object PUT  extends ReqWithContentType
  }

  final case class PathParameter(
      name: String
  )

  final case class GetRequest(
      path: String,
      pathParams: List[PathParameter],
      queries: Map[String, Primitive],
      res: Option[Map[String, Ref]])
      extends RouteDefinition

  final case class PutOrPostRequest(
      path: String,
      `type`: ReqWithContentType,
      pathParams: List[PathParameter],
      queries: Map[String, Primitive],
      req: Ref,
      res: Option[Map[String, Ref]])
      extends RouteDefinition

  final case class DeleteRequest(path: String, pathParams: List[PathParameter], res: Option[Map[String, Ref]])
      extends RouteDefinition

  // MARK: Helper functions -------------------------------------------

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
