package com.enfore.apis.generator.http4s

import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._

class ImplementationGeneratorSpec extends FreeSpec with Matchers {
  "http4s.ImplementationGenerator generates trait methods for the implementation " +
    "based on http4s according to provided route definiton" in tableTest

  def tableTest = forAll(table) { (pathItem, sourceString) =>
    ImplementationGenerator.generate(pathItem) should equal(sourceString)
  }

  lazy val table = Table(
    ("pathItem", "route"),
    `GET /contacts/individual`,
    `POST /contacts/individual`,
    `GET /contacts/individual/{contacts-id}`,
    `PUT /contacts/individual/{contacts-id}`,
    `DELETE /contacts/individual/{contacts-id}`,
    `GET /contacts/organization/{contacts-id}/addresses`,
    `POST /contacts/organization/{contacts-id}/addresses`,
    `GET /contacts/organization/{contacts-id}/addresses/{address-id}`,
    `PUT /contacts/organization/{contacts-id}/addresses/{address-id}`,
    `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`,
    `GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    `POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    `GET /contacts/individual/{contacts-id}?optional1&optional2`,
    `PUT /contacts/individual/{contacts-id}?optional1&optional2`,
    `DELETE /contacts/individual/{contacts-id}?optional1&optional2`,
    `GET /contacts/organization/{contacts-id}/addresses?query1&query2`,
    `POST /contacts/organization/{contacts-id}/addresses?query1&query2`,
    `GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    `PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    `GET /contacts/individual?optional-list1&optional-list2`,
    `POST /contacts/individual?optionaoListl1&optional-list2`,
    `GET /contacts/individual/funny.,argument/type/?other:@funny&trait`,
    `POST /contacts/single`
  )

  lazy val `GET /contacts/individual` = (
    RouteDefinitions.`GET /contacts/individual`,
    """def `GET /contacts/individual`(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `POST /contacts/individual` = (
    RouteDefinitions.`POST /contacts/individual`,
    """def `POST /contacts/individual`(body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `GET /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`GET /contacts/individual/{contacts-id}`,
    """def `GET /contacts/individual/{contacts-id}`(`contacts-id`: String)(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `PUT /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`PUT /contacts/individual/{contacts-id}`,
    """def `PUT /contacts/individual/{contacts-id}`(`contacts-id`: String, body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `DELETE /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`DELETE /contacts/individual/{contacts-id}`,
    """def `DELETE /contacts/individual/{contacts-id}`(`contacts-id`: String)(implicit request: Request[F]): F[Unit]"""
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses`,
    """def `GET /contacts/organization/{contacts-id}/addresses`(`contacts-id`: String)(implicit request: Request[F]): F[Address]"""
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses` = (
    RouteDefinitions.`POST /contacts/organization/{contacts-id}/addresses`,
    """def `POST /contacts/organization/{contacts-id}/addresses`(`contacts-id`: String, body: Address)(implicit request: Request[F]): F[Address]"""
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses/{address-id}`,
    """def `GET /contacts/organization/{contacts-id}/addresses/{address-id}`(`contacts-id`: String, `address-id`: String)(implicit request: Request[F]): F[Address]"""
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}`,
    """def `PUT /contacts/organization/{contacts-id}/addresses/{address-id}`(`contacts-id`: String, `address-id`: String, body: Address)(implicit request: Request[F]): F[Address]"""
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`,
    """def `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`(`contacts-id`: String, `address-id`: String)(implicit request: Request[F]): F[Unit]"""
  )

  lazy val `GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = (
    RouteDefinitions.`GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    """def `GET /contacts/individual`(list1: List[Int], `optional-list1`: Option[List[String]], optional1: Option[String], optional2: Option[Int], query1: String, query2: Int)(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = (
    RouteDefinitions.`POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    """def `POST /contacts/individual`(list1: List[Int], `optional-list1`: Option[List[String]], optional1: Option[String], optional2: Option[Int], query1: String, query2: Int, body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `GET /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`GET /contacts/individual/{contacts-id}?optional1&optional2`,
    """def `GET /contacts/individual/{contacts-id}`(`contacts-id`: String, optional1: Option[String], optional2: Option[Int])(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `PUT /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`PUT /contacts/individual/{contacts-id}?optional1&optional2`,
    """def `PUT /contacts/individual/{contacts-id}`(`contacts-id`: String, optional1: Option[String], optional2: Option[Int], body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
  )

  // DELETE does not support query parameters
  lazy val `DELETE /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`DELETE /contacts/individual/{contacts-id}?optional1&optional2`,
    """def `DELETE /contacts/individual/{contacts-id}`(`contacts-id`: String)(implicit request: Request[F]): F[Unit]"""
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses?query1&query2` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses?query1&query2`,
    """def `GET /contacts/organization/{contacts-id}/addresses`(`contacts-id`: String, query1: String, query2: Int)(implicit request: Request[F]): F[Address]"""
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses?query1&query2` = (
    RouteDefinitions.`POST /contacts/organization/{contacts-id}/addresses?query1&query2`,
    """def `POST /contacts/organization/{contacts-id}/addresses`(`contacts-id`: String, query1: String, query2: Int, body: Address)(implicit request: Request[F]): F[Address]"""
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    """def `GET /contacts/organization/{contacts-id}/addresses/{address-id}`(`contacts-id`: String, `address-id`: String, list1: List[String], list2: List[Int])(implicit request: Request[F]): F[Address]"""
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    """def `PUT /contacts/organization/{contacts-id}/addresses/{address-id}`(`contacts-id`: String, `address-id`: String, list1: List[String], list2: List[Int], body: Address)(implicit request: Request[F]): F[Address]"""
  )

  // DELETE does not support query parameters
  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    """def `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`(`contacts-id`: String, `address-id`: String)(implicit request: Request[F]): F[Unit]"""
  )

  lazy val `GET /contacts/individual?optional-list1&optional-list2` = (
    RouteDefinitions.`GET /contacts/individual?optional-list1&optional-list2`,
    """def `GET /contacts/individual`(`optional-list1`: Option[List[String]], `optional-list2`: Option[List[Int]])(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `POST /contacts/individual?optionaoListl1&optional-list2` = (
    RouteDefinitions.`POST /contacts/individual?optionaoListl1&optional-list2`,
    """def `POST /contacts/individual`(`optional-list1`: Option[List[String]], `optional-list2`: Option[List[Int]], body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `GET /contacts/individual/funny.,argument/type/?other:@funny&trait` = (
    RouteDefinitions.`GET /contacts/individual/funny.,argument/type/?other:@funny&trait`,
    """def `GET /contacts/individual/{funny.,argument}/{type}`(`funny.,argument`: String, `type`: String, `other:@funny`: String, `trait`: String)(implicit request: Request[F]): F[IndividualContact]"""
  )

  lazy val `POST /contacts/single` = (
    RouteDefinitions.`POST /contacts/single`,
    """def `POST /contacts/single`(body: String)(implicit request: Request[F]): F[IndividualContact]"""
  )
}
