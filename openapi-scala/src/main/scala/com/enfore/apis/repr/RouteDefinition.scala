package com.enfore.apis.repr

import cats.implicits._
import com.enfore.apis.repr.TypeRepr.{Primitive, Ref}

sealed trait RouteDefinition {
  val path: String
  val summary: Option[String]
  val operationId: Option[String]
  val pathParams: List[PathParameter]
}

final case class PathItemAggregation(path: String, items: List[RouteDefinition])

/**
  * This sum type is used to mark a request that will have content body and may
  * return a response.
  */
sealed trait ReqWithContentType
object ReqWithContentType {
  case object PATCH extends ReqWithContentType
  case object POST  extends ReqWithContentType
  case object PUT   extends ReqWithContentType
}

final case class PathParameter(
    name: String
)

final case class GetRequest(
    path: String,
    summary: Option[String],
    operationId: Option[String],
    pathParams: List[PathParameter],
    queries: Map[String, Primitive],
    response: Option[Map[String, Ref]],
    successStatusCode: Int
) extends RouteDefinition

final case class RequestWithPayload(
    path: String,
    summary: Option[String],
    operationId: Option[String],
    `type`: ReqWithContentType,
    pathParams: List[PathParameter],
    queries: Map[String, Primitive],
    request: Option[TypeRepr],
    response: Option[Map[String, Ref]],
    hasReadOnlyType: Option[Boolean],
    successStatusCode: Int
) extends RouteDefinition {

  lazy val readOnlyTypeName: Option[String] =
    (request, hasReadOnlyType).tupled.map {
      case (r, ro) =>
        if (ro) {
          s"${r.typeName}Request"
        } else {
          r.typeName
        }
    }

}

final case class DeleteRequest(
    path: String,
    summary: Option[String],
    operationId: Option[String],
    pathParams: List[PathParameter],
    response: Option[Map[String, Ref]],
    successStatusCode: Int
) extends RouteDefinition
