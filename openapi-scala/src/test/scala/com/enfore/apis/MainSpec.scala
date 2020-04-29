package com.enfore.apis

import com.enfore.apis.generator.{GenericImplementation, Http4sImplementation}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class MainSpec extends AnyFlatSpec with should.Matchers {

  it should "be able to generate scala for all yamls" in {
    val sources = List("catalog", "Contacts-API", "ERP-API", "problem", "purchasing", "registry").map(_ + ".json")
    sources.foreach { source: String =>
      info { source }
      val result = Main.generateScala(
        ClassLoader.getSystemResource(source).getPath,
        "test.package",
        List(GenericImplementation, Http4sImplementation)
      )
      result.left.map(x => println(x + s" in $source")) // For debugging the failing tests
      result.isRight shouldBe true
    }

  }
}
