package com.enfore.apis

import sbt.{Compile, Def, _}
import Keys._
import java.io.File
import java.nio.file.Files

object OpenAPIPlugin extends AutoPlugin {

  def compileCombined(
      sourceFile: File,
      outputDir: File,
      targetSvcUrl: String,
      apiDefTarget: File,
      packageName: String
  ): Seq[File] = {
    println(s"[info] Creating OpenAPI from $sourceFile and writing to target $outputDir")
    Files.createDirectories(sourceFile.getParentFile.toPath)
    Files.createDirectories(apiDefTarget.getParentFile.toPath)
    val apiCombined = CombineUtils.resolvedApi(sourceFile, targetSvcUrl)
    val jsonRepr    = CombineUtils.jsonRepr(apiCombined)
    CombineUtils.writeFile(apiDefTarget, jsonRepr)
    Main
      .genHttp4sFromJson(jsonRepr, packageName)
      .left
      .map(err => println(s"[error] Errors while converting: $err"))
      .right
      .get
      .map {
        case (name: String, content: String) =>
          val file = outputDir / s"$name.scala"
          Files.createDirectories(file.getParentFile.toPath)
          CombineUtils.writeFile(file, content.replaceAll("\t", "  "))
          file
      }
      .toSeq
  }

  object autoImport {
    val svcUrl = settingKey[String]("Stage variable OpenAPI Service endpoint. Probably should not be updated.")
    val apiDefTarget =
      settingKey[File]("Name of the base file to use for dumping OpenAPI definition in YAML and JSON.")
    val openAPISource =
      settingKey[File]("Source entry file for OpenAPI. Defaults to src/main/openapi/main.yaml")
    val openAPIOutput =
      settingKey[File]("Output directory for OpenAPI. Defaults to managed sources - openapi.")
    val openAPIOutputPackage =
      settingKey[String]("Name of the package to be used for OpenAPI generated objects.")
    val openAPIGenerate =
      taskKey[Seq[File]]("Generate Scala sources from OpenAPI definition in YAML.")
  }

  import autoImport._

  val openAPISettings: Seq[Def.Setting[_]] =
    Seq(
      svcUrl := s"$${stageVariables.serviceUri}",
      apiDefTarget := (Compile / classDirectory).value / "openapi" / (name.value + ".json"),
      //openAPISource := { sourceDirectory.value / "openapi" / "main.yaml" },
      openAPISource := { sourceDirectory.value / ".." / "main.yaml" },
      openAPIOutput := sourceManaged.value / "main",
      openAPIOutputPackage := "com.enfore.openapi",
      openAPIGenerate := Def
        .taskDyn(Def.task {
          compileCombined(
            openAPISource.value,
            openAPIOutput.value,
            svcUrl.value,
            apiDefTarget.value,
            openAPIOutputPackage.value
          )
        })
        .value,
      watchSources ++= { (openAPISource.value ** "*").get },
      sourceGenerators in Compile += openAPIGenerate
    )

  override lazy val projectSettings: Seq[Def.Setting[_]] = openAPISettings

}
