package com.enfore.apis.generator.http4s

import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr.ReqWithContentType.{PATCH, POST, PUT}
import com.enfore.apis.repr._
import scala.collection.immutable.ListMap

object RouteDefinitions {
  lazy val `GET /contacts/individual` = GetRequest(
    "/contacts/individual",
    Some("Dummy documentation\nDummy documentation next line."),
    Some("dummyFunction"),
    List.empty,
    Map.empty,
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
      )
    ),
    200
  )

  lazy val `GET /org/{org-id}/contacts/individual` = GetRequest(
    "/org/{org-id}/contacts/individual",
    None,
    None,
    List(PathParameter("org-id")),
    Map.empty,
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
      )
    ),
    200
  )

  lazy val `POST /contacts/individual` = RequestWithPayload(
    "/contacts/individual",
    None,
    None,
    POST,
    List.empty,
    Map.empty,
    Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    Some(false),
    200
  )

  lazy val `POST /contacts/single` = RequestWithPayload(
    "/contacts/single",
    None,
    None,
    POST,
    List.empty,
    Map.empty,
    Some(TypeRepr.PrimitiveString(Some(List()))),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    Some(false),
    200
  )

  lazy val `POST /contacts/individual/empty` = RequestWithPayload(
    "/contacts/individual/empty",
    None,
    None,
    POST,
    List.empty,
    Map.empty,
    None,
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    None,
    200
  )

  lazy val `GET /contacts/individual/{contacts-id}` = GetRequest(
    "/contacts/individual/{contacts-id}",
    None,
    None,
    List(PathParameter("contacts-id")),
    Map.empty,
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
      )
    ),
    200
  )

  lazy val `GET /org/{org-id}/contacts/individual/{contacts-id}` = GetRequest(
    "/org/{org-id}/contacts/individual/{contacts-id}",
    None,
    None,
    List(PathParameter("org-id"), PathParameter("contacts-id")),
    Map.empty,
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
      )
    ),
    200
  )

  lazy val `PUT /contacts/individual/{contacts-id}` = RequestWithPayload(
    "/contacts/individual/{contacts-id}",
    None,
    None,
    PUT,
    List(PathParameter("contacts-id")),
    Map.empty,
    Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    Some(false),
    200
  )

  lazy val `PUT /contacts/individual/empty/{contacts-id}` = RequestWithPayload(
    "/contacts/individual/empty/{contacts-id}",
    None,
    None,
    PUT,
    List(PathParameter("contacts-id")),
    Map.empty,
    None,
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    None,
    200
  )

  lazy val `PATCH /contacts/individual/{contacts-id}` = RequestWithPayload(
    "/contacts/individual/{contacts-id}",
    None,
    None,
    PATCH,
    List(PathParameter("contacts-id")),
    Map.empty,
    Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    Some(false),
    200
  )

  lazy val `DELETE /contacts/individual/{contacts-id}` = DeleteRequest(
    "/contacts/individual/{contacts-id}",
    None,
    None,
    List(PathParameter("contacts-id")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    200
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses` = GetRequest(
    "/contacts/organization/{contacts-id}/addresses",
    None,
    None,
    List(PathParameter("contacts-id")),
    Map.empty,
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address")
      )
    ),
    200
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses` = RequestWithPayload(
    "/contacts/organization/{contacts-id}/addresses",
    None,
    None,
    POST,
    List(PathParameter("contacts-id")),
    Map.empty,
    Some(TypeRepr.Ref("#/components/schemas/Address", "Address")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address"))),
    Some(false),
    200
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}` = GetRequest(
    "/contacts/organization/{contacts-id}/addresses/{address-id}",
    None,
    None,
    List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    Map.empty,
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address")
      )
    ),
    200
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}` = RequestWithPayload(
    "/contacts/organization/{contacts-id}/addresses/{address-id}",
    None,
    None,
    PUT,
    List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    Map.empty,
    Some(TypeRepr.Ref("#/components/schemas/Address", "Address")),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address")
      )
    ),
    Some(false),
    200
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}` = DeleteRequest(
    "/contacts/organization/{contacts-id}/addresses/{address-id}",
    None,
    None,
    List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address")
      )
    ),
    200
  )

  lazy val `GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = GetRequest(
    "/contacts/individual",
    None,
    None,
    List.empty,
    ListMap(
      "query1"         -> PrimitiveString(None),
      "query2"         -> PrimitiveInt(None),
      "optional1"      -> PrimitiveOption(PrimitiveString(None), None),
      "optional2"      -> PrimitiveOption(PrimitiveInt(None), None),
      "list1"          -> PrimitiveArray(PrimitiveInt(None), None),
      "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None), None), None)
    ),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
      )
    ),
    200
  )

  lazy val `POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = RequestWithPayload(
    "/contacts/individual",
    None,
    None,
    POST,
    List.empty,
    ListMap(
      "query1"         -> PrimitiveString(None),
      "query2"         -> PrimitiveInt(None),
      "optional1"      -> PrimitiveOption(PrimitiveString(None), None),
      "optional2"      -> PrimitiveOption(PrimitiveInt(None), None),
      "list1"          -> PrimitiveArray(PrimitiveInt(None), None),
      "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None), None), None)
    ),
    Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    Some(false),
    200
  )

  lazy val `GET /contacts/individual/{contacts-id}?optional1&optional2` = GetRequest(
    "/contacts/individual/{contacts-id}",
    None,
    None,
    List(PathParameter("contacts-id")),
    ListMap(
      "optional1" -> PrimitiveOption(PrimitiveString(None), None),
      "optional2" -> PrimitiveOption(PrimitiveInt(None), None)
    ),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
      )
    ),
    200
  )

  lazy val `PUT /contacts/individual/{contacts-id}?optional1&optional2` = RequestWithPayload(
    "/contacts/individual/{contacts-id}",
    None,
    None,
    PUT,
    List(PathParameter("contacts-id")),
    ListMap(
      "optional1" -> PrimitiveOption(PrimitiveString(None), None),
      "optional2" -> PrimitiveOption(PrimitiveInt(None), None)
    ),
    Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    Some(false),
    200
  )

  lazy val `DELETE /contacts/individual/{contacts-id}?optional1&optional2` = DeleteRequest(
    "/contacts/individual/{contacts-id}",
    None,
    None,
    List(PathParameter("contacts-id")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    200
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses?query1&query2` = GetRequest(
    "/contacts/organization/{contacts-id}/addresses",
    None,
    None,
    List(PathParameter("contacts-id")),
    ListMap(
      "query1" -> PrimitiveString(None),
      "query2" -> PrimitiveInt(None)
    ),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address")
      )
    ),
    200
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses?query1&query2` = RequestWithPayload(
    "/contacts/organization/{contacts-id}/addresses",
    None,
    None,
    POST,
    List(PathParameter("contacts-id")),
    ListMap(
      "query1" -> PrimitiveString(None),
      "query2" -> PrimitiveInt(None)
    ),
    Some(TypeRepr.Ref("#/components/schemas/Address", "Address")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address"))),
    Some(false),
    200
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = GetRequest(
    "/contacts/organization/{contacts-id}/addresses/{address-id}",
    None,
    None,
    List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    ListMap(
      "list1" -> PrimitiveArray(PrimitiveString(None), None),
      "list2" -> PrimitiveArray(PrimitiveInt(None), None)
    ),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address")
      )
    ),
    200
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = RequestWithPayload(
    "/contacts/organization/{contacts-id}/addresses/{address-id}",
    None,
    None,
    PUT,
    List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    ListMap(
      "list1" -> PrimitiveArray(PrimitiveString(None), None),
      "list2" -> PrimitiveArray(PrimitiveInt(None), None)
    ),
    Some(TypeRepr.Ref("#/components/schemas/Address", "Address")),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address")
      )
    ),
    Some(false),
    200
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = DeleteRequest(
    "/contacts/organization/{contacts-id}/addresses/{address-id}",
    None,
    None,
    List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address")
      )
    ),
    200
  )

  lazy val `GET /contacts/individual?optional-list1&optional-list2` = GetRequest(
    "/contacts/individual",
    None,
    None,
    List.empty,
    ListMap(
      "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None), None), None),
      "optional-list2" -> PrimitiveOption(PrimitiveArray(PrimitiveInt(None), None), None)
    ),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
      )
    ),
    200
  )

  lazy val `POST /contacts/individual?optionaoListl1&optional-list2` = RequestWithPayload(
    "/contacts/individual",
    None,
    None,
    POST,
    List.empty,
    ListMap(
      "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None), None), None),
      "optional-list2" -> PrimitiveOption(PrimitiveArray(PrimitiveInt(None), None), None)
    ),
    Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")),
    Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"))),
    Some(false),
    200
  )

  lazy val `GET /contacts/individual/funny.,argument/type/?other:@funny&trait` = GetRequest(
    "/contacts/individual/{funny.,argument}/{type}",
    None,
    None,
    List(
      PathParameter("funny.,argument"),
      PathParameter("type")
    ),
    Map(
      "other:@funny" -> PrimitiveString(None),
      "trait"        -> PrimitiveString(None)
    ),
    Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
      )
    ),
    200
  )
}
