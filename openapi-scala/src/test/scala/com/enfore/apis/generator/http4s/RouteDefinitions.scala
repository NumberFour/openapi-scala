package com.enfore.apis.generator.http4s

import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr._
import com.enfore.apis.repr.ReqWithContentType.{PATCH, POST, PUT}
import com.enfore.apis.repr._
import scala.collection.immutable.ListMap

object RouteDefinitions {
  lazy val `GET /contacts/individual` = GetRequest(
    path = "/contacts/individual",
    summary = Some("Dummy documentation\nDummy documentation next line."),
    description = None,
    operationId = Some("dummyFunction"),
    pathParams = List.empty,
    queries = Map.empty,
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `GET /org/{org-id}/contacts/individual` = GetRequest(
    path = "/org/{org-id}/contacts/individual",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(PathParameter("org-id")),
    queries = Map.empty,
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `POST /contacts/individual` = RequestWithPayload(
    path = "/contacts/individual",
    summary = None,
    description = None,
    operationId = None,
    `type` = POST,
    pathParams = List.empty,
    queries = Map.empty,
    request = Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)),
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `POST /contacts/single` = RequestWithPayload(
    path = "/contacts/single",
    summary = None,
    description = None,
    operationId = None,
    `type` = POST,
    pathParams = List.empty,
    queries = Map.empty,
    request = Some(TypeRepr.PrimitiveString(None, None)),
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `POST /contacts/individual/empty` = RequestWithPayload(
    path = "/contacts/individual/empty",
    summary = None,
    description = None,
    operationId = None,
    `type` = POST,
    pathParams = List.empty,
    queries = Map.empty,
    request = None,
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    hasReadOnlyType = None,
    successStatusCode = 200
  )

  lazy val `GET /contacts/individual/{contacts-id}` = GetRequest(
    path = "/contacts/individual/{contacts-id}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(PathParameter("contacts-id")),
    queries = Map.empty,
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `GET /org/{org-id}/contacts/individual/{contacts-id}` = GetRequest(
    path = "/org/{org-id}/contacts/individual/{contacts-id}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(PathParameter("org-id"), PathParameter("contacts-id")),
    queries = Map.empty,
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `PUT /contacts/individual/{contacts-id}` = RequestWithPayload(
    path = "/contacts/individual/{contacts-id}",
    summary = None,
    description = None,
    operationId = None,
    `type` = PUT,
    pathParams = List(PathParameter("contacts-id")),
    queries = Map.empty,
    request = Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)),
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `PUT /contacts/individual/empty/{contacts-id}` = RequestWithPayload(
    path = "/contacts/individual/empty/{contacts-id}",
    summary = None,
    description = None,
    operationId = None,
    `type` = PUT,
    pathParams = List(PathParameter("contacts-id")),
    queries = Map.empty,
    request = None,
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    hasReadOnlyType = None,
    successStatusCode = 200
  )

  lazy val `PATCH /contacts/individual/{contacts-id}` = RequestWithPayload(
    path = "/contacts/individual/{contacts-id}",
    summary = None,
    description = None,
    operationId = None,
    `type` = PATCH,
    pathParams = List(PathParameter("contacts-id")),
    queries = Map.empty,
    request = Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)),
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `DELETE /contacts/individual/{contacts-id}` = DeleteRequest(
    path = "/contacts/individual/{contacts-id}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(PathParameter("contacts-id")),
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    successStatusCode = 200
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses` = GetRequest(
    path = "/contacts/organization/{contacts-id}/addresses",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(PathParameter("contacts-id")),
    queries = Map.empty,
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses` = RequestWithPayload(
    path = "/contacts/organization/{contacts-id}/addresses",
    summary = None,
    description = None,
    operationId = None,
    `type` = POST,
    pathParams = List(PathParameter("contacts-id")),
    queries = Map.empty,
    request = Some(TypeRepr.Ref("#/components/schemas/Address", "Address", None)),
    response = Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None))),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}` = GetRequest(
    path = "/contacts/organization/{contacts-id}/addresses/{address-id}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    queries = Map.empty,
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}` = RequestWithPayload(
    path = "/contacts/organization/{contacts-id}/addresses/{address-id}",
    summary = None,
    description = None,
    operationId = None,
    `type` = PUT,
    pathParams = List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    queries = Map.empty,
    request = Some(TypeRepr.Ref("#/components/schemas/Address", "Address", None)),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None)
      )
    ),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}` = DeleteRequest(
    path = "/contacts/organization/{contacts-id}/addresses/{address-id}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `GET /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = GetRequest(
    path = "/contacts/individual",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List.empty,
    queries = ListMap(
      "query1"         -> PrimitiveString(None, None),
      "query2"         -> PrimitiveInt(None, None),
      "optional1"      -> PrimitiveOption(PrimitiveString(None, None)),
      "optional2"      -> PrimitiveOption(PrimitiveInt(None, None)),
      "list1"          -> PrimitiveArray(PrimitiveInt(None, None), None),
      "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None, None), None))
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `POST /contacts/individual?query1&query2&optional1&optional2&list1&optional-list1` = RequestWithPayload(
    path = "/contacts/individual",
    summary = None,
    description = None,
    operationId = None,
    `type` = POST,
    pathParams = List.empty,
    queries = ListMap(
      "query1"         -> PrimitiveString(None, None),
      "query2"         -> PrimitiveInt(None, None),
      "optional1"      -> PrimitiveOption(PrimitiveString(None, None)),
      "optional2"      -> PrimitiveOption(PrimitiveInt(None, None)),
      "list1"          -> PrimitiveArray(PrimitiveInt(None, None), None),
      "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None, None), None))
    ),
    request = Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)),
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `GET /contacts/individual/{contacts-id}?optional1&optional2` = GetRequest(
    path = "/contacts/individual/{contacts-id}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(PathParameter("contacts-id")),
    queries = ListMap(
      "optional1" -> PrimitiveOption(PrimitiveString(None, None)),
      "optional2" -> PrimitiveOption(PrimitiveInt(None, None))
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `PUT /contacts/individual/{contacts-id}?optional1&optional2` = RequestWithPayload(
    path = "/contacts/individual/{contacts-id}",
    summary = None,
    description = None,
    operationId = None,
    `type` = PUT,
    pathParams = List(PathParameter("contacts-id")),
    queries = ListMap(
      "optional1" -> PrimitiveOption(PrimitiveString(None, None)),
      "optional2" -> PrimitiveOption(PrimitiveInt(None, None))
    ),
    request = Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)),
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `DELETE /contacts/individual/{contacts-id}?optional1&optional2` = DeleteRequest(
    path = "/contacts/individual/{contacts-id}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(PathParameter("contacts-id")),
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    successStatusCode = 200
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses?query1&query2` = GetRequest(
    path = "/contacts/organization/{contacts-id}/addresses",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(PathParameter("contacts-id")),
    queries = ListMap(
      "query1" -> PrimitiveString(None, None),
      "query2" -> PrimitiveInt(None, None)
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `POST /contacts/organization/{contacts-id}/addresses?query1&query2` = RequestWithPayload(
    path = "/contacts/organization/{contacts-id}/addresses",
    summary = None,
    description = None,
    operationId = None,
    `type` = POST,
    pathParams = List(PathParameter("contacts-id")),
    queries = ListMap(
      "query1" -> PrimitiveString(None, None),
      "query2" -> PrimitiveInt(None, None)
    ),
    request = Some(TypeRepr.Ref("#/components/schemas/Address", "Address", None)),
    response = Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None))),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `GET /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = GetRequest(
    path = "/contacts/organization/{contacts-id}/addresses/{address-id}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    queries = ListMap(
      "list1" -> PrimitiveArray(PrimitiveString(None, None), None),
      "list2" -> PrimitiveArray(PrimitiveInt(None, None), None)
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `PUT /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = RequestWithPayload(
    path = "/contacts/organization/{contacts-id}/addresses/{address-id}",
    summary = None,
    description = None,
    operationId = None,
    `type` = PUT,
    pathParams = List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    queries = ListMap(
      "list1" -> PrimitiveArray(PrimitiveString(None, None), None),
      "list2" -> PrimitiveArray(PrimitiveInt(None, None), None)
    ),
    request = Some(TypeRepr.Ref("#/components/schemas/Address", "Address", None)),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None)
      )
    ),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `DELETE /contacts/organization/{contacts-id}/addresses/{address-id}?list1&list2` = DeleteRequest(
    path = "/contacts/organization/{contacts-id}/addresses/{address-id}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(
      PathParameter("contacts-id"),
      PathParameter("address-id")
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/Address", "Address", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `GET /contacts/individual?optional-list1&optional-list2` = GetRequest(
    path = "/contacts/individual",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List.empty,
    queries = ListMap(
      "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None, None), None)),
      "optional-list2" -> PrimitiveOption(PrimitiveArray(PrimitiveInt(None, None), None))
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `POST /contacts/individual?optionaoListl1&optional-list2` = RequestWithPayload(
    path = "/contacts/individual",
    summary = None,
    description = None,
    operationId = None,
    `type` = POST,
    pathParams = List.empty,
    queries = ListMap(
      "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None, None), None)),
      "optional-list2" -> PrimitiveOption(PrimitiveArray(PrimitiveInt(None, None), None))
    ),
    request = Some(TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)),
    response = Some(
      Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None))
    ),
    hasReadOnlyType = Some(false),
    successStatusCode = 200
  )

  lazy val `GET /contacts/individual/funny.,argument/type/?other:@funny&trait` = GetRequest(
    path = "/contacts/individual/{funny.,argument}/{type}",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List(
      PathParameter("funny.,argument"),
      PathParameter("type")
    ),
    queries = Map(
      "other:@funny" -> PrimitiveString(None, None),
      "trait"        -> PrimitiveString(None, None)
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `GET /contacts/individual?queryEnumList` = GetRequest(
    path = "/contacts/individual",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List.empty,
    queries = Map(
      "queryEnum" -> PrimitiveArray(
        TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None),
        None
      )
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )

  lazy val `GET /contacts/individual?queryEnum` = GetRequest(
    path = "/contacts/individual",
    summary = None,
    description = None,
    operationId = None,
    pathParams = List.empty,
    queries = Map(
      "queryEnum" -> TypeRepr.Ref(path = "#/components/schemas/IndividualContact", typeName = "IndividualContact", None)
    ),
    response = Some(
      Map(
        "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact", None)
      )
    ),
    successStatusCode = 200
  )
}
