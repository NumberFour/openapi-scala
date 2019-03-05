package com.enfore.apis

import sbt.{Def, _}
import Keys._
import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.Files

import com.enfore.apis.ast.SwaggerAST
import sbt.plugins.JvmPlugin

object OpenapiPlugin extends AutoPlugin {

  def compileOpenAPI(sourceDir: File, outputDir: File): Seq[File] = {
    println(s"Working with source $sourceDir and target $outputDir")
    if (!outputDir.exists) Files.createDirectory(outputDir.toPath)
    sourceDir.listFiles.flatMap { sourceFile =>
      SwaggerAST
        .generateScala(sourceFile.getAbsolutePath)
        .right
        .get
        .map {
          case (name: String, content: String) =>
            val file = outputDir / s"$name.scala"
            val bw   = new BufferedWriter(new FileWriter(file))
            bw.write(content)
            bw.close()
            file
        }
        .toList
    }
  }

  object autoImport {
    val openAPISource =
      SettingKey[File]("source-directory", "Source directory for OpenAPI. Defaults to src/main/openapi")
    val openAPIOutput =
      SettingKey[File]("output-directory", "Output directory for OpenAPI. Defaults to managed sources - openapi.")
    val openAPIGenerate =
      TaskKey[Seq[File]]("openapi-generate", "Generate Scala sources from OpenAPI definition in YAML.")
  }

  import autoImport._

  override def requires: JvmPlugin.type = sbt.plugins.JvmPlugin

  val OpenAPI: sbt.Configuration = config("openapi")

  val openAPISettings: Seq[Def.Setting[_]] = inConfig(OpenAPI)(
    Seq(
      openAPISource := { sourceDirectory.value / "openapi" },
      openAPIOutput := sourceManaged.value,
      openAPIGenerate := Def.taskDyn {
        Def.task {
          compileOpenAPI(openAPISource.value, openAPIOutput.value)
        }
      }.value,
    )
  ) ++ Seq(
    watchSources ++= { (openAPISource.value ** "*").get },
    sourceGenerators in Compile += (openAPIGenerate in OpenAPI).taskValue,
    ivyConfigurations += OpenAPI
  )

  override lazy val projectSettings: Seq[Def.Setting[_]] = openAPISettings

}
