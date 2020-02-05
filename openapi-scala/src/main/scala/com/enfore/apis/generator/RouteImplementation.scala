package com.enfore.apis.generator

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.generator.http4s.Http4sGenerator
import com.enfore.apis.repr.PathItemAggregation
import com.enfore.apis.generator.ScalaGenerator.ops._

trait RouteImplementation {

  /**
    * Generate the sources and returns a map of Scala file name to a string
    * containing what is supposed to be the contents of the the file in the key.
    *
    * The file is not yet written to the disk.
    */
  def generateScala(
      routesMap: Map[String, PathItemAggregation],
      packageName: PackageName
  ): Map[String, String]
}

object GenericImplementation extends RouteImplementation {
  def generateScala(
      routesMap: Map[String, PathItemAggregation],
      packageName: PackageName
  ): Map[String, String] = {
    implicit val routesGenerator: ScalaGenerator[PathItemAggregation] = PathInterfaceGenerator(packageName)
    routesMap.mapValues(_.generateScala)
  }
}

object Http4sImplementation extends RouteImplementation {
  def generateScala(
      routesMap: Map[String, PathItemAggregation],
      packageName: PackageName
  ): Map[String, String] = {
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
