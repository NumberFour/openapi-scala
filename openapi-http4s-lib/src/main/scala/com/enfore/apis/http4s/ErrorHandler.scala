package com.enfore.apis.http4s

import cats.effect.IO
import com.enfore.apis.lib._
import io.circe._
import io.circe.derivation._
import io.circe.syntax._
import org.http4s.Response
import org.http4s.circe._
import org.http4s.dsl.io._
import org.slf4j.LoggerFactory

trait ErrorHandler[F[_]] {
  def resolve[T](x: F[T], status: T => F[Response[F]]): F[Response[F]]
}

class ErrorHandlerImplementation(convertThrowableToServiceExceptionFn: Throwable => ServiceException)
    extends ErrorHandler[IO] {

  private val logger = LoggerFactory.getLogger(getClass)

  override def resolve[T](x: IO[T], status: T => IO[Response[IO]]): IO[Response[IO]] =
    resolveErrors(x).flatMap {
      case Right(value)              => status(value)
      case Left(e: ServiceException) => mapServiceErrorsFn(e)
    }

  private def resolveErrors[T](in: IO[T]): IO[Either[ServiceException, T]] =
    in.attempt
      .map(_.left.map(convertThrowableToServiceExceptionFn))

  implicit val encoder1: Encoder[ServiceError]        = deriveEncoder[ServiceError](renaming.snakeCase)
  implicit val encoder2: Encoder[ItemAlreadyExists]   = deriveEncoder[ItemAlreadyExists](renaming.snakeCase)
  implicit val encoder3: Encoder[ItemDoesNotExist]    = deriveEncoder[ItemDoesNotExist](renaming.snakeCase)
  implicit val encoder4: Encoder[RequestConflict]     = deriveEncoder[RequestConflict](renaming.snakeCase)
  implicit val encoder5: Encoder[PermissionRequired]  = deriveEncoder[PermissionRequired](renaming.snakeCase)
  implicit val encoder6: Encoder[WrongRequestContent] = deriveEncoder[WrongRequestContent](renaming.snakeCase)

  private def mapServiceErrorsFn(err: ServiceException): IO[Response[IO]] = {
    logger.error("Error in request", err)
    err match {
      case e: ServiceError =>
        InternalServerError(e.asJson)
      case e: ItemAlreadyExists =>
        Conflict(e.asJson)
      case e: ItemDoesNotExist =>
        NotFound(e.asJson)
      case e: RequestConflict =>
        Conflict(e.asJson)
      case e: PermissionRequired =>
        Forbidden(e.asJson)
      case e: WrongRequestContent =>
        BadRequest(e.asJson)
    }
  }

}
