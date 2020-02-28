package com.enfore.apis

import com.enfore.apis.ast.ASTTranslationFunctions
import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.ast.SwaggerAST._
import com.enfore.apis.generator.{Http4sImplementation, RouteImplementation, ScalaGenerator}
import io.circe
import io.circe._
import cats.syntax.functor._
import io.circe.generic.auto._

import scala.io.Source

object Main {

  implicit val decoder: Decoder[SchemaOrReferenceObject] =
    List[Decoder[SchemaOrReferenceObject]](
      Decoder[ReferenceObject].widen,
      Decoder[SchemaObject].widen
    ).reduceLeft(_ or _)

  def loadRepresentationFromFile(filename: String): Either[circe.Error, CoreASTRepr] = {
    val file = Source.fromFile(filename)
    val content = file.getLines
      .mkString("\n")
    file.close()
    parser
      .parse(content)
      .flatMap(_.as[CoreASTRepr])
  }

  def generateScala(
      filePath: String,
      packageName: String,
      routeImplementations: List[RouteImplementation]
  ): Either[circe.Error, Map[String, String]] = {
    val pn = PackageName(packageName)
    for {
      representation <- loadRepresentationFromFile(filePath)
      componentsMap = ASTTranslationFunctions.readComponentsToInterop(representation)(pn)
      routesMap     = ASTTranslationFunctions.readRoutesToInterop(representation)(pn)
    } yield {
      componentsMap.mapValues(ScalaGenerator.codeGenerator.generateScala) ++
        routeImplementations.flatMap(_.generateScala(routesMap, pn))
    }
  }
  def genHttp4sFromJson(in: String, packageName: String): Either[circe.Error, Map[String, String]] = {
    val pn = PackageName(packageName)
    for {
      repr <- parser.parse(in).flatMap(_.as[CoreASTRepr])
      componentsMap = ASTTranslationFunctions.readComponentsToInterop(repr)(pn)
      routesMap     = ASTTranslationFunctions.readRoutesToInterop(repr)(pn)
    } yield {
      componentsMap.mapValues(ScalaGenerator.codeGenerator.generateScala) ++ Http4sImplementation.generateScala(
        routesMap,
        pn
      )
    }
  }
}
