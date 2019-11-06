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

  implicit val encoderServiceError: Encoder[ServiceError] =
    deriveEncoder[ServiceError](renaming.snakeCase, None)
  implicit val encoderItemAlreadyExists: Encoder[ItemAlreadyExists] =
    deriveEncoder[ItemAlreadyExists](renaming.snakeCase, None)
  implicit val encoderObjectLocked: Encoder[ServiceObjectLocked] =
    deriveEncoder[ServiceObjectLocked](renaming.snakeCase, None)
  implicit val encoderItemDoesNotExist: Encoder[ItemDoesNotExist] =
    deriveEncoder[ItemDoesNotExist](renaming.snakeCase, None)
  implicit val encoderRequestConflict: Encoder[RequestConflict] =
    deriveEncoder[RequestConflict](renaming.snakeCase, None)
  implicit val encoderPermissionRequired: Encoder[PermissionRequired] =
    deriveEncoder[PermissionRequired](renaming.snakeCase, None)
  implicit val encoderWrongRequestContent: Encoder[WrongRequestContent] =
    deriveEncoder[WrongRequestContent](renaming.snakeCase, None)
  implicit val encoderUnprocessableContent: Encoder[UnprocessableContent] =
    deriveEncoder[UnprocessableContent](renaming.snakeCase, None)

  private def mapServiceErrorsFn(err: ServiceException): IO[Response[IO]] =
    err match {
      case e: ServiceError =>
        logger.error("ServiceError", err)
        InternalServerError(e.asJson)
      case e: ItemAlreadyExists =>
        logger.info("ItemAlreadyExists", err)
        Conflict(e.asJson)
      case e: ServiceObjectLocked =>
        logger.info("ObjectLocked", err)
        Locked(e.asJson)
      case e: ItemDoesNotExist =>
        logger.info("ItemDoesNotExist", err)
        NotFound(e.asJson)
      case e: RequestConflict =>
        logger.warn("RequestConflict", err)
        Conflict(e.asJson)
      case e: PermissionRequired =>
        logger.info("PermissionRequired", err)
        Forbidden(e.asJson)
      case e: WrongRequestContent =>
        logger.info("WrongRequestContent", err)
        BadRequest(e.asJson)
      case e: UnprocessableContent =>
        logger.info("UnprocessableContent", err)
        UnprocessableEntity(e.asJson)
    }

}
