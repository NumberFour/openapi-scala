package com.enfore.apis

import com.enfore.apis.ast.ASTTranslationFunctions
import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.ast.SwaggerAST._
import com.enfore.apis.generator.RouteImplementation
import com.enfore.apis.generator.ScalaGenerator.ops._
import io.circe
import io.circe._
import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.yaml.parser

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
    // God bless you. i.e., if you believe in God. If not, well...
      .map(line => if (line == "      - NO") "      - \"NO\"" else line)
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
      componentsMap.mapValues(_.generateScala) ++
        routeImplementations.flatMap(_.generateScala(routesMap, pn))
    }
  }
}
