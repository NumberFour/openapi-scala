package com.enfore.apis.generator.http4s

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.repr.TypeRepr
import com.enfore.apis.repr.TypeRepr._
import org.scalatest.{Assertion, FreeSpec, Matchers}

import scala.collection.immutable.ListMap
import scala.meta._

class Http4sGeneratorSpec extends FreeSpec with Matchers {
  "The Http4sGenerator generates sources which directly use and utilise http4s as a server library" - {
    "It generates a file containing the routes" in routesTest
    "It generates a file containing a trait representing the implementation used by the routes" in implementationTest
  }

  lazy val routes = ListMap[String, PathItemAggregation](
    `/contacts/individual`
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
      |import io.circe.syntax._
      |import org.http4s.circe.CirceEntityEncoder._
      |import org.http4s.circe.CirceEntityDecoder._
      |import org.http4s.dsl.Http4sDsl
      |import com.enfore.apis.http4s.ErrorHandler
      |
      |object Routes {
      |  def apply[F[_] : Sync](impl: Http4sRoutesApi[F], errorHandler: ErrorHandler[F]): HttpRoutes[F] = {
      |    val dsl = new Http4sDsl[F]{}
      |    import dsl._
      |
      |    HttpRoutes.of[F] {
      |      case request @ GET -> Root / "contacts" / "individual" =>
      |        errorHandler.resolve(impl.`GET /contacts/individual`(request), (x: IndividualContact) => Ok(x.asJson))
      |    }
      |  }
      |}
      |
     """.stripMargin.trim.parse[Source].get.structure

  lazy val implementationSource =
    """package com.enfore.contactsapiservice.contacts
      |package http4s
      |
      |import org.http4s.Request
      |
      |trait Http4sRoutesApi[F[_]] {
      |  def `GET /contacts/individual`(implicit request: Request[F]): F[IndividualContact]
      |}
     """.stripMargin.trim.parse[Source].get.structure
}
