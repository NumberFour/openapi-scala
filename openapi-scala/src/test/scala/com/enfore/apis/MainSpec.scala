package com.enfore.apis

import org.scalatest._

class MainSpec extends FlatSpec with Matchers {

  it should "be able to generate scala for all yamls" in {
    val sources = List("catalog", "Contacts-API", "ERP-API", "problem", "purchasing", "registry").map(_ + ".yaml")
    sources.foreach { source =>
      info { source }
      val result = Main.generateScala(ClassLoader.getSystemResource(source).getPath, "test.package")
      result.left.map(x => println(x + s" in $source")) // For debugging the failing tests
      result.isRight shouldBe true
    }
  }
}
