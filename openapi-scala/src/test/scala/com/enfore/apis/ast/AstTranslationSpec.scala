package com.enfore.apis.ast

import com.enfore.apis.ast.SwaggerAST.CoreASTRepr
import io.circe
import io.circe.yaml.parser
import io.circe.generic.auto._
import org.scalatest._

import scala.io.Source

class AstTranslationSpec extends FlatSpec with Matchers {

  val yamlCode: String =
    """
      |paths:
      |  '/products':
      |    post:
      |      summary: Add a new product to the organisation
      |      requestBody:
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Product'
      |      responses:
      |        201:
      |          description: Product has successfully been added
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Product'
      |      tags:
      |        - products
      |components:
      |  schemas:
      |    Money:
      |      description: A Money represents a monetary value (i.e., a currency and an amount), for example "120 EUR" or "2,500.75 USD".
      |      type: object
      |      properties:
      |        value:
      |          description: |
      |            The numerical value of the Money.
      |            Must be in the range of -9,000,000,000,000 to 9,000,000,000,000 and may use up to 6 decimal digits.
      |          type: number
      |          multipleOf: 0.000001
      |        unit:
      |          $ref: '#/components/schemas/Currency'
      |      required:
      |        - value
      |        - unit
    """.stripMargin

  "SwaggerAST" should "be translated properly to the routes" in {
    val ast = parser.parse(yamlCode).flatMap(_.as[CoreASTRepr])
    ast.left.map(println) // For debugging the failing tests
    ast.isRight shouldBe true
  }

  it should "be able to read the catalog yaml" in {
    val sources = List("catalog", "Contacts-API", "ERP-API", "problem", "purchasing", "registry").map(_ + ".yaml")
    sources.foreach { source =>
      info { source }
      val yaml: String                          = Source.fromResource(source).mkString
      val ast: Either[circe.Error, CoreASTRepr] = parser.parse(yaml).flatMap(_.as[CoreASTRepr])
      ast.left.map(x => println(x + s" in $source")) // For debugging the failing tests
      ast.isRight shouldBe true
    }
  }

}
