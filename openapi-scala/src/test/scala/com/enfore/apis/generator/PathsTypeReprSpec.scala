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
        |trait Get extends GetRequest {
        | val path = "/products"
        | val queries = Map()
        | val pathVariables = List("id")
        | 
        | type Response = com.enfore.apis.Product :+: CNil
        | type AvailableErrors = com.enfore.model.Problem :+: CNil
        | val badEncoding: IO[AvailableErrors]
        | def `application/json`(id: String): IO[com.enfore.apis.Product]
        | def impl(encoding: String)(id: String): Either[IO[AvailableErrors], IO[Response]] = encoding match {
        |   case "application/json" => Right(`application/json`(id).map(Coproduct[Response](_)))
        |   case _ => Left(badEncoding)
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
      Some(Map("application/json" -> TypeReprRef("#components/schemas/Product", "Product")))
    )
    val expected =
      """
        |trait Post extends PostRequest {
        | val path = "/products"
        | val queries = Map()
        | val pathVariables = List()
        | 
        | type Response = com.enfore.apis.Product :+: CNil
        | type AvailableErrors = com.enfore.model.Problem :+: CNil
        | val badEncoding: IO[AvailableErrors]
        | def `application/json`(req: com.enfore.apis.Product): IO[com.enfore.apis.Product]
        | def impl(encoding: String)(req: com.enfore.apis.Product): Either[IO[AvailableErrors], IO[Response]] = encoding match {
        |   case "application/json" => Right(`application/json`(req).map(Coproduct[Response](_)))
        |   case _ => Left(badEncoding)
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
      None
    )
    val expected =
      """
        |trait Post extends PostRequest {
        | val path = "/subscriptions"
        | val queries = Map()
        | val pathVariables = List()
        |
        | type Response = Unit
        | type AvailableErrors = com.enfore.model.Problem :+: CNil
        | val badEncoding: IO[AvailableErrors]
        | def impl(req: com.enfore.apis.Subscription): IO[Response]
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
      |trait Get extends GetRequest {
      | val path = "/subscriptions/{subscription-id}"
      | val queries = Map()
      | val pathVariables = List("`subscription-id`")
      | 
      | type Response = Unit
      | type AvailableErrors = com.enfore.model.Problem :+: CNil
      | val badEncoding: IO[AvailableErrors]
      | def impl(`subscription-id`: String): IO[Response]
      |}
    """.stripMargin.parse[Source]

    val generated = ScalaGenerator.routeDefGen.generateScala(request)
    generated.parse[Source].get.structure shouldBe expected.get.structure
  }

}
