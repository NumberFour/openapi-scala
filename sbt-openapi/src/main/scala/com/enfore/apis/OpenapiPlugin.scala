package com.enfore.apis

import sbt.{Def, _}
import Keys._
import java.io.{File, FileWriter}

object OpenapiPlugin extends AutoPlugin {

  def compileOpenAPI(sourceDir: File, outputDir: File, packageName: String): Seq[File] = {
    println(s"[info] Creating OpenAPI from $sourceDir and writing to target $outputDir")
    sourceDir.listFiles.flatMap { sourceFile =>
      Main
        .generateScala(sourceFile.getAbsolutePath, packageName)
        .right
        .get
        .map {
          case (name: String, content: String) =>
            val file = outputDir / s"$name.scala"
            file.getParentFile.mkdirs()
            val writer = new FileWriter(file)
            writer.write(content.replaceAll("\t", "  "))
            writer.close()
            file
        }
        .toList
    }
  }

  object autoImport {
    val openAPISource =
      settingKey[File]("Source directory for OpenAPI. Defaults to src/main/openapi")
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
      openAPISource := { sourceDirectory.value / "openapi" },
      openAPIOutput := sourceManaged.value / "main",
      openAPIOutputPackage := "com.enfore.openapi",
      openAPIGenerate := Def.taskDyn {
        Def.task {
          compileOpenAPI(openAPISource.value, openAPIOutput.value, openAPIOutputPackage.value)
        }
      }.value,
      watchSources ++= { (openAPISource.value ** "*").get },
      sourceGenerators in Compile += openAPIGenerate
    )

  override lazy val projectSettings: Seq[Def.Setting[_]] = openAPISettings

}
