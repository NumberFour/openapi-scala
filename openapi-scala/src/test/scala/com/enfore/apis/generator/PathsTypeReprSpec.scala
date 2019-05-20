package com.enfore.apis.generator

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr.{GetRequest, PathParameter, PutOrPostRequest, Ref => TypeReprRef}
import org.scalatest._

import scala.meta._

class PathsTypeReprSpec extends FlatSpec with Matchers {

  implicit val packageName: PackageName = PackageName("com.enfore.apis")

  "Paths TypeRepr Generator" should "be able to generate basic Get requests" in {
    val request = GetRequest(
      "/products",
      List(PathParameter("id")),
      Map.empty,
      Some(Map("application/json" -> TypeReprRef("#components/schemas/Product", "Product")))
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

    val generated = ScalaGenerator.routeDefGen.generateScala(request).parse[Source].get
    generated.structure shouldBe expected.get.structure
  }

  it should "be able to use the correct input/return type in post request" in {
    val request = PutOrPostRequest(
      "/products",
      TypeRepr.ReqWithContentType.POST,
      List.empty,
      Map.empty,
      TypeReprRef("#components/schemas/Product", "Product"),
      Some(Map("application/json" -> TypeReprRef("#components/schemas/Product", "Product"))),
      false
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
        | def `application/json`(req: com.enfore.apis.Product): F[com.enfore.apis.Product]
        | def impl(encoding: String)(req: com.enfore.apis.Product)(implicit ME: cats.MonadError[F, Throwable], F: cats.Functor[F]): F[Response] = encoding match {
        |   case "application/json" => F.map(`application/json`(req))(Coproduct[Response](_))
        |   case _ => ME.raiseError(EncodingMatchFailure(s"$encoding is not acceptable for $path"))
        | }
        |}
      """.stripMargin.parse[Source]

    val generated = ScalaGenerator.routeDefGen.generateScala(request)
    generated.parse[Source].get.structure shouldBe expected.get.structure
  }

  it should "be able to deal with no return type for Post requests" in {
    val request = PutOrPostRequest(
      "/subscriptions",
      TypeRepr.ReqWithContentType.POST,
      List.empty,
      Map.empty,
      TypeReprRef("#components/schemas/Subscription", "Subscription"),
      None,
      false
    )
    val expected =
      """
        |trait Post[F[_]] extends PostRequest {
        | val path = "/subscriptions"
        | val queries = Map()
        | val pathVariables = List()
        |
        | type Response = Unit
        | def impl(req: com.enfore.apis.Subscription): F[Response]
        |}
      """.stripMargin.trim.parse[Source]

    val generated = ScalaGenerator.routeDefGen.generateScala(request)
    generated.parse[Source].get.structure shouldBe expected.get.structure
  }

  it should "be able to deal with no return type for Get requests" in {
    val request = GetRequest(
      "/subscriptions/{subscription-id}",
      List(PathParameter("subscription-id")),
      Map.empty,
      None
    )
    val expected =
      """
      |trait Get[F[_]] extends GetRequest {
      | val path = "/subscriptions/{subscription-id}"
      | val queries = Map()
      | val pathVariables = List("`subscription-id`")
      | 
      | type Response = Unit
      | def impl(`subscription-id`: String): F[Response]
      |}
    """.stripMargin.parse[Source]

    val generated = ScalaGenerator.routeDefGen.generateScala(request)
    generated.parse[Source].get.structure shouldBe expected.get.structure
  }

}
