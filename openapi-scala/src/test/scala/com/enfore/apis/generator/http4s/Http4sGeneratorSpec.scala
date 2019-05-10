package com.enfore.apis.generator.http4s

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.repr.TypeRepr.ReqWithContentType.{POST, PUT}
import com.enfore.apis.repr.TypeRepr._
import org.scalatest.{Assertion, FreeSpec, Matchers}
import scala.meta._
import com.enfore.apis.repr.TypeRepr
import scala.collection.immutable.ListMap

class Http4sGeneratorSpec extends FreeSpec with Matchers {
  "The Http4sGenerator generates sources which directly use and utilise http4s as a server library" - {
    "It generates a file containing the routes" in routesTest
    "It generates a file containing a trait representing the implementation used by the routes" in implementationTest
  }

  lazy val routes = ListMap[String, PathItemAggregation](
    `/contacts/individual`,
    `/contacts/individual/queries`,
    `/contacts/individual/{contact-id}`,
    `/contacts/individual/{contact-id}/queries`,
    `/contacts/individual/{contact-id}/web-addresses/{web-address-id}`
  )

  def routesTest: Assertion = {
    val actual = Http4sGenerator
      .routes(PackageName("com.enfore.contactsapiservice.contacts"))
      .generateScala(Http4sGenerator.RoutesObject(routes))
      .parse[Source]
      .get
      .structure

    actual should equal(routesSource)
  }

  def implementationTest: Assertion = {
    val actual = Http4sGenerator
      .implementation(PackageName("com.enfore.contactsapiservice.contacts"))
      .generateScala(Http4sGenerator.ApiTrait(routes))
      .parse[Source]
      .get
      .structure

    actual should equal(implementationSource)
  }

  lazy val `/contacts/individual` = "_contacts_individual" -> PathItemAggregation(
    "/contacts/individual",
    List(
      GetRequest(
        "/contacts/individual",
        List.empty,
        Map.empty,
        Some(
          Map(
            "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
          )
        )
      ),
      PutOrPostRequest(
        "/contacts/individual",
        POST,
        List.empty,
        Map.empty,
        TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"),
        Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")))
      )
    )
  )

  lazy val `/contacts/individual/queries` = "_contacts_individual_queries" -> PathItemAggregation(
    "/contacts/individual/queries",
    List(
      GetRequest(
        "/contacts/individual/queries",
        List.empty,
        ListMap(
          "query1"         -> PrimitiveString(None),
          "query2"         -> PrimitiveInt(None),
          "query3"         -> PrimitiveInt(None),
          "optional1"      -> PrimitiveOption(PrimitiveString(None)),
          "list1"          -> PrimitiveArray(PrimitiveInt(None), None),
          "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None), None))
        ),
        Some(
          Map(
            "application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")
          )
        )
      ),
      PutOrPostRequest(
        "/contacts/individual/queries",
        POST,
        List.empty,
        ListMap(
          "query1"         -> PrimitiveString(None),
          "query2"         -> PrimitiveInt(None),
          "query3"         -> PrimitiveInt(None),
          "optional1"      -> PrimitiveOption(PrimitiveString(None)),
          "list1"          -> PrimitiveArray(PrimitiveInt(None), None),
          "optional-list1" -> PrimitiveOption(PrimitiveArray(PrimitiveString(None), None))
        ),
        TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"),
        Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")))
      )
    )
  )

  lazy val `/contacts/individual/{contact-id}` = "_contacts_individual_{contact-id}" -> PathItemAggregation(
    "/contacts/individual/{contact-id}",
    List(
      GetRequest(
        "/contacts/individual/{contact-id}",
        List(PathParameter("contact-id")),
        Map.empty,
        Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")))
      ),
      PutOrPostRequest(
        "/contacts/individual/{contact-id}",
        PUT,
        List(PathParameter("contact-id")),
        Map.empty,
        TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"),
        Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")))
      )
    )
  )

  lazy val `/contacts/individual/{contact-id}/queries` = "_contacts_individual_{contact-id}_queries" -> PathItemAggregation(
    "/contacts/individual/{contact-id}/queries",
    List(
      GetRequest(
        "/contacts/individual/{contact-id}/queries",
        List(PathParameter("contact-id")),
        ListMap(
          "query1" -> PrimitiveString(None),
          "query2" -> PrimitiveNumber(None)
        ),
        Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")))
      ),
      PutOrPostRequest(
        "/contacts/individual/{contact-id}/queries",
        PUT,
        List(PathParameter("contact-id")),
        ListMap(
          "query1" -> PrimitiveString(None),
          "query2" -> PrimitiveNumber(None)
        ),
        TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact"),
        Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/IndividualContact", "IndividualContact")))
      )
    )
  )

  lazy val `/contacts/individual/{contact-id}/web-addresses/{web-address-id}` =
    "_contacts_individual_{contact-id}_web-addresses_{web-address-id}" -> PathItemAggregation(
      "/contacts/individual/{contact-id}/web-addresses/{web-address-id}",
      List(
        DeleteRequest(
          "/contacts/individual/{contact-id}/web-addresses/{web-address-id}",
          List(PathParameter("contact-id"), PathParameter("web-address-id")),
          Some(Map("application/json" -> TypeRepr.Ref("#/components/schemas/WebAddress", "WebAddress")))
        )
      )
    )

  lazy val routesSource =
    """package com.enfore.contactsapiservice.contacts
      |package http4s
      |
      |import cats.effect.Sync
      |import cats.implicits._
      |import org.http4s._
      |import org.http4s.circe.CirceEntityEncoder._
      |import org.http4s.circe.CirceEntityDecoder._
      |import org.http4s.dsl.Http4sDsl
      |
      |object Routes {
      |  def apply[F[_] : Sync](impl: ApiImplementation[F]): HttpRoutes[F] = {
      |    val dsl = new Http4sDsl[F]{}
      |    import dsl._
      |
      |    implicit def listDecoder[T](implicit decoder: QueryParamDecoder[T]): QueryParamDecoder[List[T]] =
      |      QueryParamDecoder[String]
      |          .map(
      |            _.split(',').toList
      |                .flatMap(value => decoder.decode(QueryParameterValue(value)).toOption)
      |          )
      |
      |    object `query1 String Matcher` extends QueryParamDecoderMatcher[String]("query1")
      |    object `query2 Int Matcher` extends QueryParamDecoderMatcher[Int]("query2")
      |    object `query3 Int Matcher` extends QueryParamDecoderMatcher[Int]("query3")
      |    object `optional1 Option[String] Matcher` extends OptionalQueryParamDecoderMatcher[String]("optional1")
      |    object `list1 List[Int] Matcher` extends QueryParamDecoderMatcher[List[Int]]("list1")
      |    object `optional-list1 Option[List[String]] Matcher` extends OptionalQueryParamDecoderMatcher[List[String]]("optional-list1")
      |    object `query2 Double Matcher` extends QueryParamDecoderMatcher[Double]("query2")
      |
      |    HttpRoutes.of[F] {
      |      case GET -> Root / "contacts" / "individual" =>
      |        impl.`GET /contacts/individual`.flatMap(Ok(_))
      |
      |      case request @ POST -> Root / "contacts" / "individual" =>
      |        request.as[IndividualContact].flatMap(impl.`POST /contacts/individual`).flatMap(Ok(_))
      |
      |      case GET -> Root / "contacts" / "individual" / "queries" :? `query1 String Matcher`(query1) +& `query2 Int Matcher`(query2) +& `query3 Int Matcher`(query3) +& `optional1 Option[String] Matcher`(optional1) +& `list1 List[Int] Matcher`(list1) +& `optional-list1 Option[List[String]] Matcher`(optionalList1) =>
      |        impl.`GET /contacts/individual/queries`(list1, optionalList1, optional1, query1, query2, query3).flatMap(Ok(_))
      |
      |      case request @ POST -> Root / "contacts" / "individual" / "queries" :? `query1 String Matcher`(query1) +& `query2 Int Matcher`(query2) +& `query3 Int Matcher`(query3) +& `optional1 Option[String] Matcher`(optional1) +& `list1 List[Int] Matcher`(list1) +& `optional-list1 Option[List[String]] Matcher`(optionalList1) =>
      |        request.as[IndividualContact].flatMap(impl.`POST /contacts/individual/queries`(list1, optionalList1, optional1, query1, query2, query3, _)).flatMap(Ok(_))
      |
      |      case GET -> Root / "contacts" / "individual" / contactId =>
      |        impl.`GET /contacts/individual/{contact-id}`(contactId).flatMap(Ok(_))
      |
      |      case request @ PUT -> Root / "contacts" / "individual" / contactId =>
      |        request.as[IndividualContact].flatMap(impl.`PUT /contacts/individual/{contact-id}`(contactId, _)).flatMap(Ok(_))
      |
      |      case GET -> Root / "contacts" / "individual" / contactId / "queries" :? `query1 String Matcher`(query1) +& `query2 Double Matcher`(query2) =>
      |        impl.`GET /contacts/individual/{contact-id}/queries`(contactId, query1, query2).flatMap(Ok(_))
      |
      |      case request @ PUT -> Root / "contacts" / "individual" / contactId / "queries" :? `query1 String Matcher`(query1) +& `query2 Double Matcher`(query2) =>
      |        request.as[IndividualContact].flatMap(impl.`PUT /contacts/individual/{contact-id}/queries`(contactId, query1, query2, _)).flatMap(Ok(_))
      |
      |      case DELETE -> Root / "contacts" / "individual" / contactId / "web-addresses" / webAddressId =>
      |        impl.`DELETE /contacts/individual/{contact-id}/web-addresses/{web-address-id}`(contactId, webAddressId).flatMap(_ => NoContent())
      |    }
      |  }
      |}
      |
     """.stripMargin.trim.parse[Source].get.structure

  lazy val implementationSource =
    """package com.enfore.contactsapiservice.contacts
      |package http4s
      |
      |trait ApiImplementation[F[_]] {
      |  def `GET /contacts/individual`: F[IndividualContact]
      |  def `POST /contacts/individual`(body: IndividualContact): F[IndividualContact]
      |  def `GET /contacts/individual/queries`(list1: List[Int], `optional-list1`: Option[List[String]], optional1: Option[String], query1: String, query2: Int, query3: Int): F[IndividualContact]
      |  def `POST /contacts/individual/queries`(list1: List[Int], `optional-list1`: Option[List[String]], optional1: Option[String], query1: String, query2: Int, query3: Int, body: IndividualContact): F[IndividualContact]
      |  def `GET /contacts/individual/{contact-id}`(`contact-id`: String): F[IndividualContact]
      |  def `PUT /contacts/individual/{contact-id}`(`contact-id`: String, body: IndividualContact): F[IndividualContact]
      |  def `GET /contacts/individual/{contact-id}/queries`(`contact-id`: String, query1: String, query2: Double): F[IndividualContact]
      |  def `PUT /contacts/individual/{contact-id}/queries`(`contact-id`: String, query1: String, query2: Double, body: IndividualContact): F[IndividualContact]
      |  def `DELETE /contacts/individual/{contact-id}/web-addresses/{web-address-id}`(`contact-id`: String, `web-address-id`: String): F[Unit]
      |}
     """.stripMargin.trim.parse[Source].get.structure
}
