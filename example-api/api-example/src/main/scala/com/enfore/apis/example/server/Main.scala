package com.enfore.apis.example.server

import cats.implicits._
import org.http4s.implicits._
import cats.effect._
import com.enfore.apis.example.impl.ApiImpl
import com.enfore.apis.http4s.ErrorHandlerImplementation
import com.enfore.apis.lib.ServiceError
import com.enfore.openapi.example.http4s.Routes
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val serviceImpl = new ApiImpl()
    val errHandler = new ErrorHandlerImplementation(exception =>
      ServiceError("Unknown Error", exception.getMessage, "", None))
    val routes = Routes(serviceImpl, errHandler).orNotFound
    val serverBuilder =
      BlazeServerBuilder[IO].bindHttp(8080, "0.0.0.0").withHttpApp(routes)
    serverBuilder.serve.compile.drain.as(ExitCode.Success)
  }
}
