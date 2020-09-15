package com.enfore.apis.ast

import com.enfore.apis.repr.TypeRepr.PrimitiveValue
import enumeratum._

import scala.collection.immutable

// This attempts to be an exact representation of the OpenAPI specification https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.1.0.md
// One must not try to "improve" this structure as we load these objects directly from JSON.

object SwaggerAST {

  // --- Helper Types ---
  sealed trait RequestType
  object RequestType {
    case object PATCH  extends RequestType
    case object POST   extends RequestType
    case object PUT    extends RequestType
    case object GET    extends RequestType
    case object DELETE extends RequestType
  }

  //  --- Types for ComponentsObject ---

  sealed trait SchemaObjectType extends EnumEntry
  object SchemaObjectType extends Enum[SchemaObjectType] with CirceEnum[SchemaObjectType] {
    val values: immutable.IndexedSeq[SchemaObjectType] = findValues
    case object `object`  extends SchemaObjectType
    case object `string`  extends SchemaObjectType
    case object `integer` extends SchemaObjectType
    case object `number`  extends SchemaObjectType
    case object `boolean` extends SchemaObjectType
    case object `array`   extends SchemaObjectType
    case object `dict`    extends SchemaObjectType
  }

  /*
   * This is a marker trait for all type definitions that are possible in Swagger.
   */
  sealed trait SchemaOrReferenceObject {
    val default: Option[PrimitiveValue]
    val readOnly: Option[Boolean]
    val writeOnly: Option[Boolean]
  }

  final case class Discriminator(propertyName: String)

  final case class SchemaObject(
      summary: Option[String] = None,          // Optional field (short docstring)
      description: Option[String] = None,      // Optional field (docstring)
      `type`: Option[SchemaObjectType] = None, // Only present when property is not a reference
      // CAVEAT: We allow a reference here, which according to the spec is not allowed but the way all OpenAPI tools work
      properties: Option[Map[String, SchemaOrReferenceObject]] = None, // Only present when schema object is an object
      additionalProperties: Option[SchemaOrReferenceObject] = None,
      // CAVEAT: We allow a reference here, which according to the spec is not allowed but the way all OpenAPI tools work
      items: Option[SchemaOrReferenceObject] = None, // Only present when schema object is an array
      oneOf: Option[List[ReferenceObject]] = None,   // Used for sum types and nothing else
      discriminator: Option[Discriminator] = None,   // Used for sum types and nothing else
      enum: Option[List[String]] = None,
      required: Option[List[String]] = None,
      readOnly: Option[Boolean] = None,  // Points out whether a property is readOnly (defaults to false)
      writeOnly: Option[Boolean] = None, // Points out whether a property is writeOnly (defaults to false)
      minLength: Option[Int] = None,     // Optional refinement
      maxLength: Option[Int] = None,     // Optional refinement
      maxItems: Option[Int] = None,      // Optional refinement
      minItems: Option[Int] = None,      // Optional refinement
      maxProperties: Option[Int] = None, // Optional refinement
      minProperties: Option[Int] = None, // Optional refinement
      maximum: Option[Int] = None,       // Optional refinement
      minimum: Option[Int] = None,       // Optional refinement
      default: Option[PrimitiveValue] = None
  ) extends SchemaOrReferenceObject

  /*
   *  Example : {{{
   *   schema:
   *     $ref: '...'
   *     default: NONE // An optional field (used only for Enum references)
   *     readOnly: true // An optional field (used only for Enum references)
   *     writeOnly: false // An optional field (used only for Enum references)
   *  }}}
   *
   * Note: Our modelling is a bit pedestrian
   */
  final case class ReferenceObject(
      $ref: String,
      default: Option[PrimitiveValue],
      readOnly: Option[Boolean] = None,
      writeOnly: Option[Boolean] = None
  ) extends SchemaOrReferenceObject

  // --- Types for Routes ---

  /*
   * Example : {{{
   *   application/json:
   *     schema:
   *       $ref: ...
   * }}}
   */
  final case class MediaTypeObject(schema: Option[SchemaOrReferenceObject])

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
  final case class ParameterObject(
      name: String,
      in: ParameterLocation,
      schema: Option[SchemaOrReferenceObject],
      description: Option[String],
      required: Option[Boolean] = None,
      deprecated: Option[Boolean] = None,
      allowEmptyValue: Option[Boolean] = None,
      content: Option[Map[String, MediaTypeObject]] = None
  )

  final case class HeaderObject(
      schema: Option[ReferenceObject],
      description: Option[String]
  )

  /*
   * Example:  {{{
   *   post:
   *     summary: Some description
   *     operationId: createSomething
   *     requestBody:
   *       content: ...
   *     responses:
   *        200:
   *          content: ...
   * }}}
   */

  type ResponsesObject = Map[Int, ResponseObject]

  final case class OperationObject(
      summary: Option[String],
      description: Option[String],
      operationId: Option[String],
      requestBody: Option[RequestBodyObject],
      responses: ResponsesObject,
      parameters: Option[List[ParameterObject]]
  )

  /*
   * Example: {{{
   *   description: A complex object array response
   *   content:
   *     application/json:
   *       schema:
   *         type: array
   *         items:
   *           $ref: '#/components/schemas/VeryComplexType'
   * }}}
   */
  final case class ResponseObject(
      description: String,
      headers: Option[Map[String, HeaderObject]],
      content: Option[Map[String, MediaTypeObject]]
      // currently not supported:
      // links: Map[String, LinkObject]
  )

  final case class RequestBodyObject(
      description: Option[String],
      content: Map[String, MediaTypeObject],
      required: Option[Boolean]
  )

  // --- Aggregated types  ---

  final case class ComponentsObject(
      schemas: Map[String, SchemaObject] = Map.empty,
      responses: Option[Map[String, ResponseObject]] = None,
      parameters: Option[Map[String, ParameterObject]] = None,
      requestBodies: Option[Map[String, RequestBodyObject]] = None,
      headers: Option[Map[String, HeaderObject]] = None
  )

  final case class CoreASTRepr(components: ComponentsObject, paths: Option[Map[String, Map[String, OperationObject]]])

}
