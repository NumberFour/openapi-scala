package com.enfore.apis.generator

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.generator.RouteImplementation.{FileName, ScalaSourceContent}
import com.enfore.apis.generator.http4s.Http4sGenerator
import com.enfore.apis.repr.PathItemAggregation
import com.enfore.apis.generator.ScalaGenerator.ops._

object RouteImplementation {
  type FileName           = String
  type ScalaSourceContent = String
}

trait RouteImplementation {
  def generateScala(
      routesMap: Map[FileName, PathItemAggregation],
      packageName: PackageName
  ): Map[FileName, ScalaSourceContent]
}

object GenericImplementation extends RouteImplementation {
  def generateScala(
      routesMap: Map[FileName, PathItemAggregation],
      packageName: PackageName
  ): Map[FileName, ScalaSourceContent] = {
    implicit val routesGenerator: ScalaGenerator[PathItemAggregation] = PathInterfaceGenerator(packageName)
    routesMap.mapValues(_.generateScala)
  }
}

object Http4sImplementation extends RouteImplementation {
  def generateScala(
      routesMap: Map[FileName, PathItemAggregation],
      packageName: PackageName
  ): Map[FileName, ScalaSourceContent] = {
    implicit val http4sRoutesGenerator: ScalaGenerator[Http4sGenerator.RoutesObject] =
      Http4sGenerator.routes(packageName)

    implicit val http4sImplementationGenerator: ScalaGenerator[Http4sGenerator.ApiTrait] =
      Http4sGenerator.implementation(packageName)

    Map(
      "http4s/Http4sRoutes"    -> Http4sGenerator.RoutesObject(routesMap).generateScala,
      "http4s/Http4sRoutesApi" -> Http4sGenerator.ApiTrait(routesMap).generateScala
    )
  }
}
