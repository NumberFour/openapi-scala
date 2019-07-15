package com.enfore.apis.lib

// This is hardcoded for know. In case we often change the Problem type, we have to generate everything that depends on it
sealed trait ServiceException extends RuntimeException {
  val `type`: String
  val title: String
  val detail: String
  val onResource: Option[String]

  override def getMessage: String = s"type: ${`type`} title: $title, detail: $detail, onResource: $onResource,"
}

final case class ServiceError(
    `type`: String,
    title: String,
    detail: String,
    onResource: Option[String]
) extends ServiceException

final case class ItemAlreadyExists(
    `type`: String,
    title: String = "Item already exists",
    detail: String,
    onResource: Option[String]
) extends ServiceException

final case class ItemDoesNotExist(
    `type`: String,
    title: String = "Item does not exist",
    detail: String,
    onResource: Option[String]
) extends ServiceException

final case class RequestConflict(
    `type`: String,
    title: String = "Service encountered a conflict while performing the operation.",
    detail: String,
    onResource: Option[String]
) extends ServiceException

final case class PermissionRequired(
    `type`: String,
    title: String = "You don't have sufficient permissions to access this resource.",
    detail: String,
    onResource: Option[String]
) extends ServiceException

final case class WrongRequestContent(
    `type`: String,
    title: String = "BadRequest: Please check your input.",
    detail: String,
    onResource: Option[String]
) extends ServiceException

final case class UnprocessableContent(
    `type`: String,
    title: String = "Specified data is semantically incorrect.",
    detail: String,
    onResource: Option[String]
) extends ServiceException
