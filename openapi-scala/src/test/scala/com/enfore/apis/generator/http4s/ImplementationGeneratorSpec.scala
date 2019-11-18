package com.enfore.apis.generator.http4s

import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._

class ImplementationGeneratorSpec extends FreeSpec with Matchers {
  "http4s.ImplementationGenerator generates trait methods for the implementation " +
    "based on http4s according to provided route definiton" in tableTest

  def tableTest = forAll(table) { (pathItem, sourceString) =>
    ImplementationGenerator.generate(pathItem, indentationLevel = 0) should equal(sourceString)
  }

  lazy val table = Table(
    ("pathItem", "route"),
    `GET /contacts/individual`,
    `POST /contacts/individual`,
    `GET /contacts/individual/{contacts-id}`,
    `PUT /contacts/individual/{contacts-id}`,
    `PATCH /contacts/individual/{contacts-id}`,
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
    List(
      "/**",
      " * Dummy documentation",
      " * Dummy documentation next line.",
      "**/",
      "def dummyFunction(implicit request: Request[F]): F[IndividualContact]"
    )
  )

  lazy val `POST /contacts/individual` = (
    RouteDefinitions.`POST /contacts/individual`,
    List(
      """def `POST /contacts/individual`(body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `GET /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`GET /contacts/individual/{contacts-id}`,
    List(
      """def `GET /contacts/individual/{contacts-id}`(contactsId: String)(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `PUT /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`PUT /contacts/individual/{contacts-id}`,
    List(
      """def `PUT /contacts/individual/{contacts-id}`(contactsId: String, body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `PATCH /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`PATCH /contacts/individual/{contacts-id}`,
    List(
      """def `PATCH /contacts/individual/{contacts-id}`(contactsId: String, body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `DELETE /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`DELETE /contacts/individual/{contacts-id}`,
    List(
      """def `DELETE /contacts/individual/{contacts-id}`(contactsId: String)(implicit request: Request[F]): F[Unit]"""
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses`,
    List(
      """def `GET /contacts/organization/{contacts-id}/addresses`(contactsId: String)(implicit request: Request[F]): F[Address]"""
    )
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses` = (
    RouteDefinitions.`POST /contacts/organization/{contacts-id}/addresses`,
    List(
      """def `POST /contacts/organization/{contacts-id}/addresses`(contactsId: String, body: Address)(implicit request: Request[F]): F[Address]"""
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses/{address-id}`,
    List(
      """def `GET /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId: String, addressId: String)(implicit request: Request[F]): F[Address]"""
    )
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}`,
    List(
      """def `PUT /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId: String, addressId: String, body: Address)(implicit request: Request[F]): F[Address]"""
    )
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`,
    List(
      """def `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId: String, addressId: String)(implicit request: Request[F]): F[Unit]"""
    )
  )

  lazy val `GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = (
    RouteDefinitions.`GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    List(
      """def `GET /contacts/individual`(list1: List[Int], optionalList1: Option[List[String]], optional1: Option[String], optional2: Option[Int], query1: String, query2: Int)(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = (
    RouteDefinitions.`POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    List(
      """def `POST /contacts/individual`(list1: List[Int], optionalList1: Option[List[String]], optional1: Option[String], optional2: Option[Int], query1: String, query2: Int, body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `GET /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`GET /contacts/individual/{contacts-id}?optional1&optional2`,
    List(
      """def `GET /contacts/individual/{contacts-id}`(contactsId: String, optional1: Option[String], optional2: Option[Int])(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `PUT /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`PUT /contacts/individual/{contacts-id}?optional1&optional2`,
    List(
      """def `PUT /contacts/individual/{contacts-id}`(contactsId: String, optional1: Option[String], optional2: Option[Int], body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  // DELETE does not support query parameters
  lazy val `DELETE /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`DELETE /contacts/individual/{contacts-id}?optional1&optional2`,
    List(
      """def `DELETE /contacts/individual/{contacts-id}`(contactsId: String)(implicit request: Request[F]): F[Unit]"""
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses?query1&query2` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses?query1&query2`,
    List(
      """def `GET /contacts/organization/{contacts-id}/addresses`(contactsId: String, query1: String, query2: Int)(implicit request: Request[F]): F[Address]"""
    )
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses?query1&query2` = (
    RouteDefinitions.`POST /contacts/organization/{contacts-id}/addresses?query1&query2`,
    List(
      """def `POST /contacts/organization/{contacts-id}/addresses`(contactsId: String, query1: String, query2: Int, body: Address)(implicit request: Request[F]): F[Address]"""
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    List(
      """def `GET /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId: String, addressId: String, list1: List[String], list2: List[Int])(implicit request: Request[F]): F[Address]"""
    )
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    List(
      """def `PUT /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId: String, addressId: String, list1: List[String], list2: List[Int], body: Address)(implicit request: Request[F]): F[Address]"""
    )
  )

  // DELETE does not support query parameters
  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    List(
      """def `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId: String, addressId: String)(implicit request: Request[F]): F[Unit]"""
    )
  )

  lazy val `GET /contacts/individual?optional-list1&optional-list2` = (
    RouteDefinitions.`GET /contacts/individual?optional-list1&optional-list2`,
    List(
      """def `GET /contacts/individual`(optionalList1: Option[List[String]], optionalList2: Option[List[Int]])(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `POST /contacts/individual?optionaoListl1&optional-list2` = (
    RouteDefinitions.`POST /contacts/individual?optionaoListl1&optional-list2`,
    List(
      """def `POST /contacts/individual`(optionalList1: Option[List[String]], optionalList2: Option[List[Int]], body: IndividualContact)(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `GET /contacts/individual/funny.,argument/type/?other:@funny&trait` = (
    RouteDefinitions.`GET /contacts/individual/funny.,argument/type/?other:@funny&trait`,
    List(
      """def `GET /contacts/individual/{funny.,argument}/{type}`(`funny.,argument`: String, `type`: String, `other:@funny`: String, `trait`: String)(implicit request: Request[F]): F[IndividualContact]"""
    )
  )

  lazy val `POST /contacts/single` = (
    RouteDefinitions.`POST /contacts/single`,
    List("""def `POST /contacts/single`(body: String)(implicit request: Request[F]): F[IndividualContact]""")
  )
}
