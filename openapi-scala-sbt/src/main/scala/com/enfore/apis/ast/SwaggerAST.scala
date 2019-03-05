package com.enfore.apis.ast

import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.generator.ScalaGenerator.ops._
import enumeratum._
import cats.implicits._

import scala.collection.immutable
import scala.io.Source
import io.circe.yaml.parser
import io.circe
import io.circe.generic.auto._

object SwaggerAST {

  final case class PackageName(name: String)

  sealed trait ComponentType extends EnumEntry
  object ComponentType extends Enum[ComponentType] with CirceEnum[ComponentType] {
    val values: immutable.IndexedSeq[ComponentType] = findValues
    case object `object` extends ComponentType
    case object `string` extends ComponentType
  }

  sealed trait PropertyType extends EnumEntry
  object PropertyType extends Enum[PropertyType] with CirceEnum[PropertyType] {
    val values: immutable.IndexedSeq[PropertyType] = findValues
    case object `object` extends PropertyType
    case object string   extends PropertyType
    case object boolean  extends PropertyType
    case object array    extends PropertyType
    case object number   extends PropertyType
  }

  final case class Property(
      description: Option[String], // Optional field (docstring)
      `type`: Option[PropertyType], // Only present when property is not a reference
      items: Option[Property], // Only present when property is a sequence
      $ref: Option[String]) // Reference to another type

  final case class Component(
      description: Option[String],
      `type`: ComponentType,
      properties: Option[Map[String, Property]],
      enum: Option[List[String]],
      required: Option[List[String]])

  final case class SchemaStore(schemas: Map[String, Component])
  final case class CoreASTRepr(components: SchemaStore)

  def loadReprFromFile(filename: String): Either[circe.Error, CoreASTRepr] = {
    val file    = Source.fromFile(filename)
    val content = file.getLines.mkString("\n")
    file.close()
    parser
      .parse(content)
      .flatMap(_.as[CoreASTRepr])
  }

  def loadPropertyObject(pType: Option[PropertyType], param: Option[Property])(
      implicit packageName: PackageName): Option[TypeRepr] =
    pType flatMap {
      case PropertyType.`object` => None
      case PropertyType.string   => PrimitiveString.some
      case PropertyType.boolean  => PrimitiveBoolean.some
      case PropertyType.array =>
        assert(param.isDefined, "A parameter type for an array is not defined")
        loadSingleProperty(param.get).map(PrimitiveArray).widen[TypeRepr]
      case PropertyType.number => PrimitiveNumber.some
    }

  def loadSingleProperty(property: Property)(implicit packageName: PackageName): Option[TypeRepr] =
    property.$ref.fold(loadPropertyObject(property.`type`, property.items)) { ref =>
      val name: String = ref.split("/").last
      val path: String = ref.split("/").dropRight(1).mkString(".")
      (Ref(path.replace("#.components.schemas", packageName.name), name): TypeRepr).some
    }

  def makeSymbolFromTypeRepr(name: String, repr: TypeRepr): Symbol = repr match {
    case p: Primitive => PrimitiveSymbol(name, p)
    case n: NewType   => NewTypeSymbol(name, n)
    case r: Ref       => RefSymbol(name, r)
  }

  def loadObjectProperties(typeName: String, properties: Map[String, Property], required: List[String])(
      implicit packageName: PackageName): Option[NewType] = {
    val mapped: immutable.Iterable[Option[Symbol]] = properties.mapValues(loadSingleProperty) map {
      case (name: String, repr: Option[TypeRepr]) =>
        val mapOp = if (required.contains(name)) repr else repr.map(PrimitiveOption)
        mapOp map (makeSymbolFromTypeRepr(name, _))
    }
    mapped.toList.sequence.map(PrimitiveProduct(packageName.name, typeName, _))
  }

  def loadEnum(typeName: String, values: List[String])(implicit packageName: PackageName): NewType =
    PrimitiveEnum(packageName.name, typeName, values.toSet)

  def evalSchema(name: String, component: Component)(implicit packageName: PackageName): Option[Symbol] = {
    val required = component.required.getOrElse(List.empty[String])
    val newType: Option[NewType] = component.`type` match {
      case ComponentType.`object` => component.properties.flatMap(loadObjectProperties(name, _, required))
      case ComponentType.`string` => component.enum.map(loadEnum(name, _))
    }
    newType.map(NewTypeSymbol(name, _))
  }

  def readToTypeRepr(ast: CoreASTRepr)(implicit packageName: PackageName): Map[String, Symbol] =
    ast.components.schemas
      .flatMap(x => evalSchema(x._1, x._2).map(x._1 -> _))
      .toList
      .toMap

  def generateScala(filePath: String): Either[circe.Error, Map[String, String]] =
    loadReprFromFile(filePath)
      .map(readToTypeRepr(_)(PackageName("com.enfore.apis"))) // TODO: Fix this shim
      .map(_.map(x => x._1 -> x._2.generateScala))

}
