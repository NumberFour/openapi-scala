package com.enfore.apis.generator.http4s

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.generator.RouteImplementation.FileName
import com.enfore.apis.generator.ScalaGenerator
import com.enfore.apis.repr.TypeRepr.PathItemAggregation

object Http4sGenerator {

  /**
    * Case class used as input to create Scala source which holds an object for creating http4s.HttpRoutes
    */
  final case class RoutesObject(routesMap: Map[FileName, PathItemAggregation])

  /**
    * Case class used as input to create Scala source which holds a trait for the API implementation
    */
  final case class ApiTrait(routesMap: Map[FileName, PathItemAggregation])

  /**
    * The generated Routes object will look something like this:
    *
    * {{{
    * object Routes {
    *   def appply[F[_] : Sync](impl: ApiImplementation[F]): HttpRoutes[F] = {
    *     val dsl = new Http4sDsl[F]{}
    *     import dsl._
    *
    *     HttpRoutes.of[F] {
    *       case GET -> Root / "contacts" / "individual" =>
    *         impl.`GET /contacts/individual`.flatMap(Ok(_))
    *
    *       case request @ POST -> Root / "contacts" / "individual" =>
    *         request.as[IndividualContact].flatMap(impl.`POST /contacts/individual`).flatMap(Ok(_))
    *     }
    *   }
    * }
    * }}}
    *
    * The implicit {{{Sync}}} parameter is needed for {{{HttpRoutes.of[F]}}}
    *
    * @param packageName The package where the Routes object should reside
    * @return A String with the Routes object's implementation as Scala Source
    */
  def routes(packageName: PackageName): ScalaGenerator[RoutesObject] = {
    case RoutesObject(routes) =>
      if (routes.isEmpty) {
        ""
      } else {
        s"""package ${packageName.name}
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
         |${queryParameterMatchers(routes, indentationLevel = 2).mkString("\n")}
         |    HttpRoutes.of[F] {
         |${routeDefinitions(routes, indentationLevel = 3).mkString("\n\n")}
         |    }
         |  }
         |}
     """.stripMargin
      }
  }

  /**
    * The generated ApiImplementation trait will look something like this:
    *
    * {{{
    * trait ApiImplementation[F[_]] {
    *   def `GET /contacts/individual`: F[IndividualContact]
    *   def `POST /contacts/individual`(body: IndividualContact): F[IndividualContact]
    * }
    * }}}
    *
    * @param packageName The package where the ApiImplementation trait should reside
    * @return A String with the ApiImplementation trait's definition as Scala Source
    */
  def implementation(
      packageName: PackageName
  ): ScalaGenerator[ApiTrait] = {
    case ApiTrait(routes) =>
      s"""package ${packageName.name}
         |package http4s
         |
         |import org.http4s.Request
         |
         |trait ApiImplementation[F[_]] {
         |${implementationTrait(routes).map("  " + _).mkString("\n")}
         |}
      """.stripMargin
  }

  /**
    * Create query parameter decoder matchers from given routes if needed.
    * It will include a self-baked List[T] decoder, if any of the parameter types is a list
    * @param indentationLevel set the level of indentation for the scala source string
    */
  private def queryParameterMatchers(
      routes: Map[FileName, PathItemAggregation],
      indentationLevel: Int
  ): List[String] = {
    val routeDefinitions = routes.values.toList.flatMap(_.items)

    val listDecoder = RouteGenerator.listDecoder(routeDefinitions, indentationLevel)
    val matchers    = RouteGenerator.buildMatchers(routeDefinitions, indentationLevel)

    (listDecoder, matchers) match {
      case (Nil, m) => m
      case (l, m)   => (l :+ "\n") ++ m
    }
  }

  private def routeDefinitions(routes: Map[FileName, PathItemAggregation], indentationLevel: Int): List[String] =
    routes.values.toList
      .flatMap(_.items)
      .map(RouteGenerator.generate(_, indentationLevel).mkString("\n"))

  private def implementationTrait(routes: Map[FileName, PathItemAggregation]): List[String] =
    routes.values.toList.flatMap(_.items).map(ImplementationGenerator.generate)
}
