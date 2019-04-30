package com.enfore.apis.generator.http4s

import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._

class RouteGeneratorSpec extends FreeSpec with Matchers {
  "http4s.RouteGenerator generates routes based on http4s according to provided route definiton" in tableTest

  def tableTest = forAll(table) { (pathItem, sourceStrings) =>
    RouteGenerator.generate(pathItem, indentationLevel = 0) should equal(sourceStrings)
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
    `GET /contacts/individual/funny.,argument/type/?other:@funny&trait`
  )

  lazy val `GET /contacts/individual` = (
    RouteDefinitions.`GET /contacts/individual`,
    List(
      """case GET -> Root / "contacts" / "individual" =>""",
      """  impl.`GET /contacts/individual`.flatMap(Ok(_))"""
    )
  )

  lazy val `POST /contacts/individual` = (
    RouteDefinitions.`POST /contacts/individual`,
    List(
      """case request @ POST -> Root / "contacts" / "individual" =>""",
      """  request.as[IndividualContact].flatMap(impl.`POST /contacts/individual`).flatMap(Ok(_))"""
    )
  )

  lazy val `GET /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`GET /contacts/individual/{contacts-id}`,
    List(
      """case GET -> Root / "contacts" / "individual" / contactsId =>""",
      "  impl.`GET /contacts/individual/{contacts-id}`(contactsId).flatMap(Ok(_))"
    )
  )

  lazy val `PUT /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`PUT /contacts/individual/{contacts-id}`,
    List(
      """case request @ PUT -> Root / "contacts" / "individual" / contactsId =>""",
      "  request.as[IndividualContact].flatMap(impl.`PUT /contacts/individual/{contacts-id}`(contactsId, _)).flatMap(Ok(_))"
    )
  )

  lazy val `DELETE /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`DELETE /contacts/individual/{contacts-id}`,
    List(
      """case DELETE -> Root / "contacts" / "individual" / contactsId =>""",
      "  impl.`DELETE /contacts/individual/{contacts-id}`(contactsId).flatMap(_ => NoContent())"
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses`,
    List(
      """case GET -> Root / "contacts" / "organization" / contactsId / "addresses" =>""",
      "  impl.`GET /contacts/organization/{contacts-id}/addresses`(contactsId).flatMap(Ok(_))"
    )
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses` = (
    RouteDefinitions.`POST /contacts/organization/{contacts-id}/addresses`,
    List(
      """case request @ POST -> Root / "contacts" / "organization" / contactsId / "addresses" =>""",
      "  request.as[Address].flatMap(impl.`POST /contacts/organization/{contacts-id}/addresses`(contactsId, _)).flatMap(Ok(_))"
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses/{address-id}`,
    List(
      """case GET -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId =>""",
      "  impl.`GET /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId).flatMap(Ok(_))"
    )
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}`,
    List(
      """case request @ PUT -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId =>""",
      "  request.as[Address].flatMap(impl.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId, _)).flatMap(Ok(_))"
    )
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`,
    List(
      """case DELETE -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId =>""",
      "  impl.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId).flatMap(_ => NoContent())"
    )
  )

  lazy val `GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = (
    RouteDefinitions.`GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    List(
      """case GET -> Root / "contacts" / "individual" :? `query1 String Matcher`(query1) +& `query2 Int Matcher`(query2) +& `optional1 Option[String] Matcher`(optional1) +& `optional2 Option[Int] Matcher`(optional2) +& `list1 List[Int] Matcher`(list1) +& `optional-list1 Option[List[String]] Matcher`(optionalList1) =>""",
      "  impl.`GET /contacts/individual`(list1, optionalList1, optional1, optional2, query1, query2).flatMap(Ok(_))"
    )
  )

  lazy val `POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = (
    RouteDefinitions.`POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    List(
      """case request @ POST -> Root / "contacts" / "individual" :? `query1 String Matcher`(query1) +& `query2 Int Matcher`(query2) +& `optional1 Option[String] Matcher`(optional1) +& `optional2 Option[Int] Matcher`(optional2) +& `list1 List[Int] Matcher`(list1) +& `optional-list1 Option[List[String]] Matcher`(optionalList1) =>""",
      "  request.as[IndividualContact].flatMap(impl.`POST /contacts/individual`(list1, optionalList1, optional1, optional2, query1, query2, _)).flatMap(Ok(_))"
    )
  )

  lazy val `GET /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`GET /contacts/individual/{contacts-id}?optional1&optional2`,
    List(
      """case GET -> Root / "contacts" / "individual" / contactsId :? `optional1 Option[String] Matcher`(optional1) +& `optional2 Option[Int] Matcher`(optional2) =>""",
      "  impl.`GET /contacts/individual/{contacts-id}`(contactsId, optional1, optional2).flatMap(Ok(_))"
    )
  )

  lazy val `PUT /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`PUT /contacts/individual/{contacts-id}?optional1&optional2`,
    List(
      """case request @ PUT -> Root / "contacts" / "individual" / contactsId :? `optional1 Option[String] Matcher`(optional1) +& `optional2 Option[Int] Matcher`(optional2) =>""",
      "  request.as[IndividualContact].flatMap(impl.`PUT /contacts/individual/{contacts-id}`(contactsId, optional1, optional2, _)).flatMap(Ok(_))"
    )
  )

  lazy val `DELETE /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`DELETE /contacts/individual/{contacts-id}?optional1&optional2`,
    List(
      """case DELETE -> Root / "contacts" / "individual" / contactsId =>""",
      "  impl.`DELETE /contacts/individual/{contacts-id}`(contactsId).flatMap(_ => NoContent())"
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses?query1&query2` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses?query1&query2`,
    List(
      """case GET -> Root / "contacts" / "organization" / contactsId / "addresses" :? `query1 String Matcher`(query1) +& `query2 Int Matcher`(query2) =>""",
      "  impl.`GET /contacts/organization/{contacts-id}/addresses`(contactsId, query1, query2).flatMap(Ok(_))"
    )
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses?query1&query2` = (
    RouteDefinitions.`POST /contacts/organization/{contacts-id}/addresses?query1&query2`,
    List(
      """case request @ POST -> Root / "contacts" / "organization" / contactsId / "addresses" :? `query1 String Matcher`(query1) +& `query2 Int Matcher`(query2) =>""",
      "  request.as[Address].flatMap(impl.`POST /contacts/organization/{contacts-id}/addresses`(contactsId, query1, query2, _)).flatMap(Ok(_))"
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    List(
      """case GET -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId :? `list1 List[String] Matcher`(list1) +& `list2 List[Int] Matcher`(list2) =>""",
      "  impl.`GET /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId, list1, list2).flatMap(Ok(_))"
    )
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    List(
      """case request @ PUT -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId :? `list1 List[String] Matcher`(list1) +& `list2 List[Int] Matcher`(list2) =>""",
      "  request.as[Address].flatMap(impl.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId, list1, list2, _)).flatMap(Ok(_))"
    )
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    List(
      """case DELETE -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId =>""",
      "  impl.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId).flatMap(_ => NoContent())"
    )
  )

  lazy val `GET /contacts/individual?optional-list1&optional-list2` = (
    RouteDefinitions.`GET /contacts/individual?optional-list1&optional-list2`,
    List(
      """case GET -> Root / "contacts" / "individual" :? `optional-list1 Option[List[String]] Matcher`(optionalList1) +& `optional-list2 Option[List[Int]] Matcher`(optionalList2) =>""",
      "  impl.`GET /contacts/individual`(optionalList1, optionalList2).flatMap(Ok(_))"
    )
  )

  lazy val `POST /contacts/individual?optionaoListl1&optional-list2` = (
    RouteDefinitions.`POST /contacts/individual?optionaoListl1&optional-list2`,
    List(
      """case request @ POST -> Root / "contacts" / "individual" :? `optional-list1 Option[List[String]] Matcher`(optionalList1) +& `optional-list2 Option[List[Int]] Matcher`(optionalList2) =>""",
      "  request.as[IndividualContact].flatMap(impl.`POST /contacts/individual`(optionalList1, optionalList2, _)).flatMap(Ok(_))"
    )
  )

  lazy val `GET /contacts/individual/funny.,argument/type/?other:@funny&trait` = (
    RouteDefinitions.`GET /contacts/individual/funny.,argument/type/?other:@funny&trait`,
    List(
      """case GET -> Root / "contacts" / "individual" / funny_Argument / typeParameter :? `other:@funny String Matcher`(other_Funny) +& `trait String Matcher`(traitParameter) =>""",
      "  impl.`GET /contacts/individual/{funny.,argument}/{type}`(funny_Argument, typeParameter, other_Funny, traitParameter).flatMap(Ok(_))"
    )
  )
}
