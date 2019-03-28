package com.enfore.apis

import com.enfore.apis.ast.ASTTranslationFunctions
import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.ast.SwaggerAST._
import com.enfore.apis.generator.ScalaGenerator
import com.enfore.apis.generator.ScalaGenerator.ops._
import com.enfore.apis.repr.TypeRepr._
import io.circe
import io.circe.generic.auto._
import io.circe.yaml.parser

import scala.io.Source

object Main {

  def loadRepresentationFromFile(filename: String): Either[circe.Error, CoreASTRepr] = {
    val file    = Source.fromFile(filename)
    val content = file.getLines.mkString("\n")
    file.close()
    parser
      .parse(content)
      .flatMap(_.as[CoreASTRepr])
  }

  def generateScala(filePath: String, packageName: String): Either[circe.Error, Map[String, String]] = {
    val pn                                                            = PackageName(packageName)
    implicit val routesGenerator: ScalaGenerator[PathItemAggregation] = ScalaGenerator.pathInterfaceGen(pn)
    for {
      representation <- loadRepresentationFromFile(filePath)
      componentsMap = ASTTranslationFunctions.readComponentsToInterop(representation)(pn)
      routesMap     = ASTTranslationFunctions.readRoutesToInerop(representation)
    } yield componentsMap.mapValues(_.generateScala) ++ routesMap.mapValues(_.generateScala)
  }
}
