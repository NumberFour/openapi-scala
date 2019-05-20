package com.enfore.apis.ast

import enumeratum._
import scala.collection.immutable

object SwaggerAST {

  // --- Helper Types ---
  sealed trait RequestType
  object RequestType {
    case object POST   extends RequestType
    case object PUT    extends RequestType
    case object GET    extends RequestType
    case object DELETE extends RequestType
  }

  //  --- Types for Components ---

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
      $ref: Option[String], // Reference to another type
      readOnly: Option[Boolean], // Points out whether a property is readOnly (defaults to false)
      minLength: Option[Int], // Optional refinement of minimum length
      maxLength: Option[Int] // Optional refinement of maximum length
  )

  final case class Component(
      description: Option[String],
      `type`: ComponentType,
      properties: Option[Map[String, Property]],
      enum: Option[List[String]],
      required: Option[List[String]]
  )

  // --- Types for Routes ---

  /*
   *  Example : {{{
   *   schema:
   *     $ref: '...'
   *  }}}
   */
  final case class SchemaObject($ref: Option[String])

  /*
   * Example : {{{
   *   application/json:
   *     schema:
   *       $ref: ...
   * }}}
   */
  final case class SchemaRefContainer(schema: Option[SchemaObject])

  /*
   * Example : {{{
   *   content:
   *     application/json:
   *       schema: ...
   * }}}
   */
  final case class MediaTypeObject(content: Option[Map[String, SchemaRefContainer]])

  sealed trait ParameterLocation extends EnumEntry
  object ParameterLocation extends Enum[ParameterLocation] with CirceEnum[ParameterLocation] {
    val values: immutable.IndexedSeq[ParameterLocation] = findValues
    case object path   extends ParameterLocation
    case object query  extends ParameterLocation
    case object header extends ParameterLocation
    case object cookie extends ParameterLocation
  }

  /*
   * Example: {{{
   *   parameters:
   *     - name: id
   *       in: path
   *       description: ...
   * }}}
   */
  final case class ParamObject(
      name: String,
      in: ParameterLocation,
      schema: Option[SchemaObject],
      description: Option[String]
  )

  /*
   * Example:  {{{
   *   post:
   *     summary: Some description
   *     requestBody:
   *       content: ...
   *     responses:
   *        200:
   *          content: ...
   * }}}
   */
  final case class PathObject(
      summary: Option[String],
      requestBody: Option[MediaTypeObject],
      responses: Map[Int, MediaTypeObject],
      parameters: Option[List[ParamObject]]
  )

  // --- Aggregated types  ---

  final case class SchemaStore(schemas: Map[String, Component])

  final case class CoreASTRepr(components: SchemaStore, paths: Option[Map[String, Map[String, PathObject]]])

}
