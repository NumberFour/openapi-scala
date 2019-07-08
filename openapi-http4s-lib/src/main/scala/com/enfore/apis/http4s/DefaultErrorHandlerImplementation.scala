package com.enfore.apis.http4s

import com.enfore.apis.lib.{ServiceError, ServiceException}
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal

object DefaultErrorHandlerImplementation {
  private val logger = LoggerFactory.getLogger(getClass)

  lazy val defaultConverter: Throwable => ServiceException = {
    case se: ServiceException => se

    case NonFatal(e) =>
      logger.error("Service Error", e)
      ServiceError(
        "ServiceError",
        e.getClass.getName,
        e.getMessage,
        None
      )
  }

  def apply: ErrorHandlerImplementation = new ErrorHandlerImplementation(defaultConverter)
}
