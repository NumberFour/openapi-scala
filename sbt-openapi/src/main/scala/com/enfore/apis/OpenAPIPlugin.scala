package com.enfore.apis

import sbt._
import Keys._
import java.io.File
import java.nio.file._

import org.scalafmt.interfaces.Scalafmt

object OpenAPIPlugin extends AutoPlugin {

  val scalafmt = Scalafmt.create(this.getClass.getClassLoader)

  def compileAll(
      extraSourcesJarName: Option[File],
      hostProjectOpenAPISourceDir: File,
      openAPIEntryPointFile: File,
      generatedCodeOutputDir: File,
      targetSvcUrl: String,
      apiDefTarget: File,
      packageName: String
  ): Seq[File] = {
    println(
      s"[info] Creating OpenAPI from $hostProjectOpenAPISourceDir/$openAPIEntryPointFile and writing to target $generatedCodeOutputDir"
    )
    if (hostProjectOpenAPISourceDir.exists() && hostProjectOpenAPISourceDir.isDirectory) {
      IO.withTemporaryDirectory { tmpDir =>
        extraSourcesJarName.foreach(IO.unzip(_, tmpDir))
        IO.copyDirectory(hostProjectOpenAPISourceDir, tmpDir)
        println("[info] Available OpenAPI Files:")
        println(tmpDir.list.mkString("[info] ", "\n[info] ", ""))
        Files.createDirectories(apiDefTarget.getParentFile.toPath)
        val apiCombined = CombineUtils.resolvedApi(new File(tmpDir, openAPIEntryPointFile.name), targetSvcUrl)
        val jsonRepr    = CombineUtils.jsonRepr(apiCombined)
        CombineUtils.writeFile(apiDefTarget, jsonRepr)
        Main
          .genHttp4sFromJson(jsonRepr, packageName)
          .left
          .map(err => println(s"[error] Errors while convertiong: $err"))
          .right
          .get
          .map {
            case (name: String, content: String) =>
              val file = generatedCodeOutputDir / s"$name.scala"
              Files.createDirectories(file.getParentFile.toPath)
              val writable = scalafmt.format(
                java.nio.file.Path.of(ClassLoader.getSystemClassLoader.getResource("scalafmt.conf").toURI),
                file.toPath,
                content
              )
              CombineUtils.writeFile(file, writable)
              file
          }
          .toSeq
      }
    } else Seq.empty
  }

  object autoImport {
    val svcUrl = settingKey[String]("Stage variable OpenAPI Service endpoint. Probably should not be updated.")
    val apiDefTarget =
      settingKey[File]("Name of the base file to use for dumping OpenAPI definition in YAML and JSON.")
    val openAPISource =
      settingKey[File](
        "Source entry directory for OpenAPI (expects to contain main.yaml file). Defaults to src/main/openapi"
      )
    val openAPISourceFile =
      settingKey[File]("Source entry file that is inside the source entry directory. Defaults to main.yaml.")
    val extraSourcesJar =
      settingKey[String]("Name of the Jar/Zip file that contains extra YAML definitions for shared objects")
    val openAPIOutput =
      settingKey[File]("Output directory for OpenAPI. Defaults to managed sources - openapi.")
    val openAPIOutputPackage =
      settingKey[String]("Name of the package to be used for OpenAPI generated objects.")
    val openAPIBuild = taskKey[Seq[File]]("Generate Scala sources from OpenAPI including shared forigen objects.")
  }

  import autoImport._

  val openAPISettings: Seq[Def.Setting[_]] =
    Seq(
      svcUrl := s"$${stageVariables.serviceUri}",
      apiDefTarget := (Compile / classDirectory).value / "openapi" / (name.value + ".json"),
      openAPISource := { sourceDirectory.value / ".." / "main.yaml" },
      openAPIOutput := sourceManaged.value / "main",
      openAPIOutputPackage := "com.enfore.openapi",
      openAPIBuild := Def
        .taskDyn(Def.task {
          compileAll(
            Def.settingDyn {
              (dependencyClasspath in Compile)
                .map(
                  _.find((x: Attributed[File]) => x.data.getName.contains(extraSourcesJar.value))
                    .map(_.data)
                )
            }.value,
            openAPISource.value,
            openAPISourceFile.value,
            openAPIOutput.value,
            svcUrl.value,
            apiDefTarget.value,
            openAPIOutputPackage.value
          )
        })
        .value,
      watchSources ++= { (openAPISource.value ** "*").get }
    )

  override lazy val projectSettings: Seq[Def.Setting[_]] = openAPISettings

}
