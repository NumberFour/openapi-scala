package com.enfore.apis

import java.io.{File, FileWriter}

import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem}
import io.swagger.v3.core.util.{Json, Yaml}
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.{AuthorizationValue, ParseOptions}

import scala.collection.JavaConverters._

object CombineUtils {

  def jsonRepr(in: OpenAPI): String = Json.pretty(in)

  def yamlRepr(in: OpenAPI): String = Yaml.pretty(in)

  def writeFile(file: File, content: String): Unit = {
    println("[info] Writing file: " + file.getPath)
    val writer = new FileWriter(file)
    writer.write(content)
    writer.close()
  }

  def resolvedApi(in: File, targetSvcUrl: String): OpenAPI = {
    val parseResult = (new OpenAPIV3Parser)
      .readLocation(
        in.getAbsolutePath,
        List.empty[AuthorizationValue].asJava,
        parseOptions
      )
    val errors: List[String] = Option(parseResult.getMessages)
      .fold(List.empty[String])(
        _.asScala.toList
      )
    if (errors.nonEmpty) {
      sys.error(
        s"Could not parse parse YAML file to OpenAPI due to error(s): \n  ${errors.mkString("  \n")}"
      )
    }
    extendWithAWS(parseResult.getOpenAPI, targetSvcUrl)
  }

  private lazy val parseOptions: ParseOptions = {
    val options = new ParseOptions
    options.setResolve(true)
    options.setResolveCombinators(true)
    options.setResolveFully(false)
    options.setFlatten(false)
    options
  }

  private def extendWithAWS(openApi: OpenAPI, targetServiceUrl: String): OpenAPI = {
    openApi.getPaths.asScala.foreach {
      case (path, pathItem) =>
        setExtensionsForPathItem(path, pathItem, targetServiceUrl)
    }
    openApi
  }

  private def setExtensionsForPathItem(path: String, pathItem: PathItem, targetServiceUrl: String): Unit =
    List(
      Option(pathItem.getGet).map((_, "GET")),
      Option(pathItem.getPut).map((_, "PUT")),
      Option(pathItem.getPost).map((_, "POST")),
      Option(pathItem.getDelete).map((_, "DELETE")),
      Option(pathItem.getOptions).map((_, "OPTIONS")),
      Option(pathItem.getHead).map((_, "HEAD")),
      Option(pathItem.getPatch).map((_, "PATCH")),
      Option(pathItem.getTrace).map((_, "TRACE"))
    ).flatten
      .foreach {
        case (operationObject, methodName) =>
          setExtensionsForOperation(
            methodName,
            path,
            operationObject,
            targetServiceUrl
          )
      }

  private def setExtensionsForOperation(
      method: String,
      path: String,
      operation: Operation,
      targetServiceUrl: String
  ): Unit = {
    val purifiedPath = path.replaceFirst("^/", "")
    val params = Option(operation.getParameters.asScala)
      .getOrElse(List.empty)
      .filter(_.getIn == "path")
      .map(_.getName)
      .map(paramName => s"integration.request.path.$paramName" -> s"method.request.path.$paramName")
    val extensions = javaMap(
      "x-amazon-apigateway-integration" -> javaMap(
        "uri" -> s"http://$targetServiceUrl/$purifiedPath",
        "responses" -> javaMap(
          "default" -> javaMap(
            "statusCode" -> "200"
          )
        ),
        "requestParameters" -> javaMap(
          params: _*
        ),
        "passthroughBehavior" -> "when_no_match",
        "connectionType"      -> "VPC_LINK",
        "connectionId"        -> s"$${stageVariables.vpcLinkId}",
        "httpMethod"          -> method,
        "type"                -> "http_proxy"
      )
    )
    operation.extensions(extensions)
    ()
  }

  private def javaMap(values: (String, AnyRef)*): java.util.Map[String, AnyRef] =
    values.toMap.asJava
}
