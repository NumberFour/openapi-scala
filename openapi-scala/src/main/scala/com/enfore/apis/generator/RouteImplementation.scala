package com.enfore.apis.generator

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.generator.http4s.Http4sGenerator
import com.enfore.apis.repr.PathItemAggregation

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
    val routesGenerator: ScalaGenerator[PathItemAggregation] = PathInterfaceGenerator(packageName)
    routesMap.mapValues(routesGenerator.generateScala)
  }
}

object Http4sImplementation extends RouteImplementation {
  def generateScala(
      routesMap: Map[String, PathItemAggregation],
      packageName: PackageName
  ): Map[String, String] = {
    val http4sRoutesGenerator: ScalaGenerator[Http4sGenerator.RoutesObject] =
      Http4sGenerator.routes(packageName)

    val http4sImplementationGenerator: ScalaGenerator[Http4sGenerator.ApiTrait] =
      Http4sGenerator.implementation(packageName)

    Map(
      "http4s/Http4sRoutes"    -> http4sRoutesGenerator.generateScala(Http4sGenerator.RoutesObject(routesMap)),
      "http4s/Http4sRoutesApi" -> http4sImplementationGenerator.generateScala(Http4sGenerator.ApiTrait(routesMap))
    )
  }
}
