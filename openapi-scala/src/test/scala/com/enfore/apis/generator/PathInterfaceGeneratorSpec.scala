package com.enfore.apis.generator

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.repr.TypeRepr.{Ref => TypeReprRef}
import com.enfore.apis.repr.{GetRequest, PathParameter, RequestWithPayload, ReqWithContentType}
import org.scalatest._

import scala.meta._

class PathInterfaceGeneratorSpec extends FlatSpec with Matchers {

  implicit val packageName: PackageName = PackageName("com.enfore.apis")

  "Paths TypeRepr Generator" should "be able to generate basic Get requests" in {
    val request = GetRequest(
      "/products",
      None,
      Some("dummyFunction"),
      List(PathParameter("id")),
      Map.empty,
      Some(Map("application/json" -> TypeReprRef("#components/schemas/Product", "Product"))),
      200
    )
    val expected =
      """
        |trait Get[F[_]] extends GetRequest {
        | val path = "/products"
        | val queries = Map()
        | val pathVariables = List("id")
        |
        | import shapeless._
        | type Response = com.enfore.apis.Product :+: CNil
        | def `application/json`(id: String): F[com.enfore.apis.Product]
        | def impl(encoding: String)(id: String)(implicit ME: cats.MonadError[F, Throwable], F: cats.Functor[F]): F[Response] = encoding match {
        |   case "application/json" => F.map(`application/json`(id))(Coproduct[Response](_))
        |   case _ => ME.raiseError(EncodingMatchFailure(s"$encoding is not acceptable for $path"))
        | }
        |}
      """.stripMargin.parse[Source]

    val generated = PathInterfaceGenerator.routeDefGen.generateScala(request).parse[Source].get
    generated.structure shouldBe expected.get.structure
  }

  it should "be able to use the correct input/return type in post request" in {
    val request = RequestWithPayload(
      "/products",
      None,
      Some("dummyFunction"),
      ReqWithContentType.POST,
      List.empty,
      Map.empty,
      Some(TypeReprRef("#components/schemas/Product", "Product")),
      Some(Map("application/json" -> TypeReprRef("#components/schemas/Product", "Product"))),
      Some(false),
      200
    )
    val expected =
      """
        |trait Post[F[_]] extends PostRequest {
        | val path = "/products"
        | val queries = Map()
        | val pathVariables = List()
        |
        | import shapeless._
        | type Response = com.enfore.apis.Product :+: CNil
        | def `application/json`(request: com.enfore.apis.Product): F[com.enfore.apis.Product]
        | def impl(encoding: String)(request: com.enfore.apis.Product)(implicit ME: cats.MonadError[F, Throwable], F: cats.Functor[F]): F[Response] = encoding match {
        |   case "application/json" => F.map(`application/json`(request))(Coproduct[Response](_))
        |   case _ => ME.raiseError(EncodingMatchFailure(s"$encoding is not acceptable for $path"))
        | }
        |}
      """.stripMargin.parse[Source]

    val generated = PathInterfaceGenerator.routeDefGen.generateScala(request)
    generated.parse[Source].get.structure shouldBe expected.get.structure
  }

  it should "be able to deal with no return type for Post requests" in {
    val request = RequestWithPayload(
      "/subscriptions",
      None,
      Some("dummyFunction"),
      ReqWithContentType.POST,
      List.empty,
      Map.empty,
      Some(TypeReprRef("#components/schemas/Subscription", "Subscription")),
      None,
      Some(false),
      200
    )
    val expected =
      """
        |trait Post[F[_]] extends PostRequest {
        | val path = "/subscriptions"
        | val queries = Map()
        | val pathVariables = List()
        |
        | type Response = Unit
        | def impl(request: com.enfore.apis.Subscription): F[Response]
        |}
      """.stripMargin.trim.parse[Source]

    val generated = PathInterfaceGenerator.routeDefGen.generateScala(request)
    generated.parse[Source].get.structure shouldBe expected.get.structure
  }

  it should "be able to deal with no return type for Get requests" in {
    val request = GetRequest(
      "/subscriptions/{subscription-id}",
      None,
      Some("dummyFunction"),
      List(PathParameter("subscriptionId")),
      Map.empty,
      None,
      200
    )
    val expected =
      """
      |trait Get[F[_]] extends GetRequest {
      | val path = "/subscriptions/{subscription-id}"
      | val queries = Map()
      | val pathVariables = List("subscriptionId")
      |
      | type Response = Unit
      | def impl(subscriptionId: String): F[Response]
      |}
    """.stripMargin.parse[Source]

    val generated = PathInterfaceGenerator.routeDefGen.generateScala(request)
    generated.parse[Source].get.structure shouldBe expected.get.structure
  }

}
