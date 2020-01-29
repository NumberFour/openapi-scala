package com.enfore.apis.example.impl

import cats.effect._
import cats.effect.concurrent.Ref
import com.enfore.openapi.example.{HelloRegisterContainer, HelloRegisterContainerRequest, HelloWrapper}
import com.enfore.openapi.example.http4s.Http4sRoutesApi
import org.http4s.Request

class ApiImpl extends Http4sRoutesApi[IO] {

  val helloIndex: Ref[IO, Map[String, String]] =
    Ref.unsafe[IO, Map[String, String]](Map.empty[String, String])

  /**
    * Register a new hello type for the given name
   **/
  override def registerHello(name: String, body: HelloRegisterContainerRequest)(
      implicit request: Request[IO]): IO[HelloRegisterContainer] =
    helloIndex
      .update(_ + (name -> body.message.value))
      .map(_ => HelloRegisterContainer(request.hashCode().toString, body.message))

  /**
    * Say hello.
   **/
  override def getHello(name: String)(implicit request: Request[IO]): IO[HelloWrapper] = helloIndex.get.map { index =>
    val hello = index.getOrElse(name, "Hello")
    HelloWrapper(HelloWrapper.RefinementConstructors.content.unsafeFrom(s"$hello $name"))
  }
}
