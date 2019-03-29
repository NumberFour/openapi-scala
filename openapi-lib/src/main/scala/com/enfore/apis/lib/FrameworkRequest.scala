package com.enfore.apis.lib

/**
  * Marker trait for all the HTTP request types
  */
trait FrameworkRequest {
  val path: String
  val queries: Map[String, String]
  val pathVariables: List[String]

  type Response
}

trait GetRequest    extends FrameworkRequest
trait DeleteRequest extends FrameworkRequest
trait PostRequest   extends FrameworkRequest
trait PutRequest    extends FrameworkRequest
trait PatchRequest  extends FrameworkRequest

final case class EncodingMatchFailure(message: String) extends Exception(message)
