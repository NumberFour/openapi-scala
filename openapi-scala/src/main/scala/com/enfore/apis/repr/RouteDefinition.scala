package com.enfore.apis.repr

import com.enfore.apis.repr.TypeRepr.{Primitive, Ref}

sealed trait RouteDefinition {
  val path: String
  val pathParams: List[PathParameter]
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
    response: Option[Map[String, Ref]],
    successStatusCode: Int
) extends RouteDefinition

final case class PutOrPostRequest(
    path: String,
    `type`: ReqWithContentType,
    pathParams: List[PathParameter],
    queries: Map[String, Primitive],
    request: Ref,
    response: Option[Map[String, Ref]],
    hasReadOnlyType: Boolean,
    successStatusCode: Int
) extends RouteDefinition {

  lazy val readOnlyTypeName: String =
    if (hasReadOnlyType) {
      s"${request.typeName}Request"
    } else {
      request.typeName
    }

}

final case class DeleteRequest(
    path: String,
    pathParams: List[PathParameter],
    response: Option[Map[String, Ref]],
    successStatusCode: Int
) extends RouteDefinition
