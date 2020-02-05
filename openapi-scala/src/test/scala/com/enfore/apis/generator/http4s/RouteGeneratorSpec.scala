package com.enfore.apis.generator.http4s

import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._

class RouteGeneratorSpec extends FreeSpec with Matchers {
  "http4s.RouteGenerator generates routes based on http4s according to provided route definiton" in tableTest

  def tableTest = forEvery(table) { (pathItem, sourceStrings) =>
    RouteGenerator.generate(pathItem, indentationLevel = 0) should equal(sourceStrings)
  }

  lazy val table = Table(
    ("pathItem", "route"),
    `GET /contacts/individual`,
    `GET /org/{org-id}/contacts/individual`,
    `POST /contacts/individual`,
    `POST /contacts/individual/empty`,
    `GET /contacts/individual/{contacts-id}`,
    `GET /org/{org-id}/contacts/individual/{contacts-id}`,
    `PUT /contacts/individual/{contacts-id}`,
    `PUT /contacts/individual/empty/{contacts-id}`,
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
      """case request @ GET -> Root / "contacts" / "individual" =>""",
      """  errorHandler.resolve(impl.dummyFunction(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"""
    )
  )

  lazy val `GET /org/{org-id}/contacts/individual` = (
    RouteDefinitions.`GET /org/{org-id}/contacts/individual`,
    List(
      """case request @ GET -> Root / "org" / orgId / "contacts" / "individual" =>""",
      """  errorHandler.resolve(impl.`GET /org/{org-id}/contacts/individual`(orgId)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"""
    )
  )

  lazy val `POST /contacts/individual` = (
    RouteDefinitions.`POST /contacts/individual`,
    List(
      """case request @ POST -> Root / "contacts" / "individual" =>""",
      """  errorHandler.resolve(request.as[IndividualContact].flatMap(impl.`POST /contacts/individual`(_)(request)), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"""
    )
  )

  lazy val `POST /contacts/individual/empty` = (
    RouteDefinitions.`POST /contacts/individual/empty`,
    List(
      """case request @ POST -> Root / "contacts" / "individual" / "empty" =>""",
      """  errorHandler.resolve(impl.`POST /contacts/individual/empty`(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"""
    )
  )

  lazy val `GET /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`GET /contacts/individual/{contacts-id}`,
    List(
      """case request @ GET -> Root / "contacts" / "individual" / contactsId =>""",
      "  errorHandler.resolve(impl.`GET /contacts/individual/{contacts-id}`(contactsId)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `GET /org/{org-id}/contacts/individual/{contacts-id}` = (
    RouteDefinitions.`GET /org/{org-id}/contacts/individual/{contacts-id}`,
    List(
      """case request @ GET -> Root / "org" / orgId / "contacts" / "individual" / contactsId =>""",
      "  errorHandler.resolve(impl.`GET /org/{org-id}/contacts/individual/{contacts-id}`(orgId, contactsId)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `PUT /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`PUT /contacts/individual/{contacts-id}`,
    List(
      """case request @ PUT -> Root / "contacts" / "individual" / contactsId =>""",
      "  errorHandler.resolve(request.as[IndividualContact].flatMap(impl.`PUT /contacts/individual/{contacts-id}`(contactsId, _)(request)), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `PUT /contacts/individual/empty/{contacts-id}` = (
    RouteDefinitions.`PUT /contacts/individual/empty/{contacts-id}`,
    List(
      """case request @ PUT -> Root / "contacts" / "individual" / "empty" / contactsId =>""",
      "  errorHandler.resolve(impl.`PUT /contacts/individual/empty/{contacts-id}`(contactsId)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `PATCH /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`PATCH /contacts/individual/{contacts-id}`,
    List(
      """case request @ PATCH -> Root / "contacts" / "individual" / contactsId =>""",
      "  errorHandler.resolve(request.as[IndividualContact].flatMap(impl.`PATCH /contacts/individual/{contacts-id}`(contactsId, _)(request)), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `DELETE /contacts/individual/{contacts-id}` = (
    RouteDefinitions.`DELETE /contacts/individual/{contacts-id}`,
    List(
      """case request @ DELETE -> Root / "contacts" / "individual" / contactsId =>""",
      "  errorHandler.resolve(impl.`DELETE /contacts/individual/{contacts-id}`(contactsId)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses`,
    List(
      """case request @ GET -> Root / "contacts" / "organization" / contactsId / "addresses" =>""",
      "  errorHandler.resolve(impl.`GET /contacts/organization/{contacts-id}/addresses`(contactsId)(request), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses` = (
    RouteDefinitions.`POST /contacts/organization/{contacts-id}/addresses`,
    List(
      """case request @ POST -> Root / "contacts" / "organization" / contactsId / "addresses" =>""",
      "  errorHandler.resolve(request.as[Address].flatMap(impl.`POST /contacts/organization/{contacts-id}/addresses`(contactsId, _)(request)), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses/{address-id}`,
    List(
      """case request @ GET -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId =>""",
      "  errorHandler.resolve(impl.`GET /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId)(request), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}`,
    List(
      """case request @ PUT -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId =>""",
      "  errorHandler.resolve(request.as[Address].flatMap(impl.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId, _)(request)), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}` = (
    RouteDefinitions.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`,
    List(
      """case request @ DELETE -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId =>""",
      "  errorHandler.resolve(impl.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId)(request), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = (
    RouteDefinitions.`GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    List(
      """case request @ GET -> Root / "contacts" / "individual" :? `query1 QueryParamDecoderMatcher[String] Matcher`(query1) +& `query2 QueryParamDecoderMatcher[Int] Matcher`(query2) +& `optional1 OptionalQueryParamDecoderMatcher[String] Matcher`(optional1) +& `optional2 OptionalQueryParamDecoderMatcher[Int] Matcher`(optional2) +& `list1 QueryParamDecoderMatcher[List[Int]] Matcher`(list1) +& `optional-list1 OptionalQueryParamDecoderMatcher[List[String]] Matcher`(optionalList1) =>""",
      "  errorHandler.resolve(impl.`GET /contacts/individual`(list1, optionalList1, optional1, optional2, query1, query2)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = (
    RouteDefinitions.`POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1`,
    List(
      """case request @ POST -> Root / "contacts" / "individual" :? `query1 QueryParamDecoderMatcher[String] Matcher`(query1) +& `query2 QueryParamDecoderMatcher[Int] Matcher`(query2) +& `optional1 OptionalQueryParamDecoderMatcher[String] Matcher`(optional1) +& `optional2 OptionalQueryParamDecoderMatcher[Int] Matcher`(optional2) +& `list1 QueryParamDecoderMatcher[List[Int]] Matcher`(list1) +& `optional-list1 OptionalQueryParamDecoderMatcher[List[String]] Matcher`(optionalList1) =>""",
      "  errorHandler.resolve(request.as[IndividualContact].flatMap(impl.`POST /contacts/individual`(list1, optionalList1, optional1, optional2, query1, query2, _)(request)), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `GET /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`GET /contacts/individual/{contacts-id}?optional1&optional2`,
    List(
      """case request @ GET -> Root / "contacts" / "individual" / contactsId :? `optional1 OptionalQueryParamDecoderMatcher[String] Matcher`(optional1) +& `optional2 OptionalQueryParamDecoderMatcher[Int] Matcher`(optional2) =>""",
      "  errorHandler.resolve(impl.`GET /contacts/individual/{contacts-id}`(contactsId, optional1, optional2)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `PUT /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`PUT /contacts/individual/{contacts-id}?optional1&optional2`,
    List(
      """case request @ PUT -> Root / "contacts" / "individual" / contactsId :? `optional1 OptionalQueryParamDecoderMatcher[String] Matcher`(optional1) +& `optional2 OptionalQueryParamDecoderMatcher[Int] Matcher`(optional2) =>""",
      "  errorHandler.resolve(request.as[IndividualContact].flatMap(impl.`PUT /contacts/individual/{contacts-id}`(contactsId, optional1, optional2, _)(request)), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `DELETE /contacts/individual/{contacts-id}?optional1&optional2` = (
    RouteDefinitions.`DELETE /contacts/individual/{contacts-id}?optional1&optional2`,
    List(
      """case request @ DELETE -> Root / "contacts" / "individual" / contactsId =>""",
      "  errorHandler.resolve(impl.`DELETE /contacts/individual/{contacts-id}`(contactsId)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses?query1&query2` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses?query1&query2`,
    List(
      """case request @ GET -> Root / "contacts" / "organization" / contactsId / "addresses" :? `query1 QueryParamDecoderMatcher[String] Matcher`(query1) +& `query2 QueryParamDecoderMatcher[Int] Matcher`(query2) =>""",
      "  errorHandler.resolve(impl.`GET /contacts/organization/{contacts-id}/addresses`(contactsId, query1, query2)(request), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses?query1&query2` = (
    RouteDefinitions.`POST /contacts/organization/{contacts-id}/addresses?query1&query2`,
    List(
      """case request @ POST -> Root / "contacts" / "organization" / contactsId / "addresses" :? `query1 QueryParamDecoderMatcher[String] Matcher`(query1) +& `query2 QueryParamDecoderMatcher[Int] Matcher`(query2) =>""",
      "  errorHandler.resolve(request.as[Address].flatMap(impl.`POST /contacts/organization/{contacts-id}/addresses`(contactsId, query1, query2, _)(request)), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    List(
      """case request @ GET -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId :? `list1 QueryParamDecoderMatcher[List[String]] Matcher`(list1) +& `list2 QueryParamDecoderMatcher[List[Int]] Matcher`(list2) =>""",
      "  errorHandler.resolve(impl.`GET /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId, list1, list2)(request), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    List(
      """case request @ PUT -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId :? `list1 QueryParamDecoderMatcher[List[String]] Matcher`(list1) +& `list2 QueryParamDecoderMatcher[List[Int]] Matcher`(list2) =>""",
      "  errorHandler.resolve(request.as[Address].flatMap(impl.`PUT /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId, list1, list2, _)(request)), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = (
    RouteDefinitions.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2`,
    List(
      """case request @ DELETE -> Root / "contacts" / "organization" / contactsId / "addresses" / addressId =>""",
      "  errorHandler.resolve(impl.`DELETE /contacts/organization/{contacts-id}/addresses/{address-id}`(contactsId, addressId)(request), (x: Address) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `GET /contacts/individual?optional-list1&optional-list2` = (
    RouteDefinitions.`GET /contacts/individual?optional-list1&optional-list2`,
    List(
      """case request @ GET -> Root / "contacts" / "individual" :? `optional-list1 OptionalQueryParamDecoderMatcher[List[String]] Matcher`(optionalList1) +& `optional-list2 OptionalQueryParamDecoderMatcher[List[Int]] Matcher`(optionalList2) =>""",
      "  errorHandler.resolve(impl.`GET /contacts/individual`(optionalList1, optionalList2)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `POST /contacts/individual?optionaoListl1&optional-list2` = (
    RouteDefinitions.`POST /contacts/individual?optionaoListl1&optional-list2`,
    List(
      """case request @ POST -> Root / "contacts" / "individual" :? `optional-list1 OptionalQueryParamDecoderMatcher[List[String]] Matcher`(optionalList1) +& `optional-list2 OptionalQueryParamDecoderMatcher[List[Int]] Matcher`(optionalList2) =>""",
      "  errorHandler.resolve(request.as[IndividualContact].flatMap(impl.`POST /contacts/individual`(optionalList1, optionalList2, _)(request)), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `GET /contacts/individual/funny.,argument/type/?other:@funny&trait` = (
    RouteDefinitions.`GET /contacts/individual/funny.,argument/type/?other:@funny&trait`,
    List(
      """case request @ GET -> Root / "contacts" / "individual" / funny_Argument / typeParameter :? `other:@funny QueryParamDecoderMatcher[String] Matcher`(other_Funny) +& `trait QueryParamDecoderMatcher[String] Matcher`(traitParameter) =>""",
      "  errorHandler.resolve(impl.`GET /contacts/individual/{funny.,argument}/{type}`(funny_Argument, typeParameter, other_Funny, traitParameter)(request), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"
    )
  )

  lazy val `POST /contacts/single` = (
    RouteDefinitions.`POST /contacts/single`,
    List(
      """case request @ POST -> Root / "contacts" / "single" =>""",
      """  errorHandler.resolve(request.as[String].flatMap(impl.`POST /contacts/single`(_)(request)), (x: IndividualContact) => EntityGenerator(200)(x.asJson))"""
    )
  )
}
