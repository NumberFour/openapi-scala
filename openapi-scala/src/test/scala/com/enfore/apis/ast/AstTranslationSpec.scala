package com.enfore.apis.ast

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.ast.SwaggerAST._
import com.enfore.apis.repr.ReqWithContentType.POST
import com.enfore.apis.repr.{PathItemAggregation, PutOrPostRequest, TypeRepr}
import com.enfore.apis.repr.TypeRepr.{
  PrimitiveInt,
  PrimitiveNumber,
  PrimitiveOption,
  PrimitiveProduct,
  PrimitiveString,
  PrimitiveSymbol,
  Ref
}
import org.scalatest._
import io.circe
import io.circe._
import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.yaml.parser

import scala.io.Source

class AstTranslationSpec extends FlatSpec with Matchers {

  implicit val decoder: Decoder[SchemaOrReferenceObject] =
    List[Decoder[SchemaOrReferenceObject]](
      Decoder[ReferenceObject].widen,
      Decoder[SchemaObject].widen
    ).reduceLeft(_ or _)

  "SwaggerAST" should "be translated properly to the routes" in {
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
      |              $ref: '#/components/schemas/Money'
      |      parameters:
      |        - name: limit
      |          in: query
      |          description: Description text.
      |          schema:
      |            type: integer
      |            format: int64
      |            minimum: 1
      |            maximum: 5000
      |      responses:
      |        201:
      |          description: Product has successfully been added
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Money'
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
                  type: string
      |      required:
      |        - value
      |        - unit
    """.stripMargin

    val ast = parser.parse(yamlCode).flatMap(_.as[CoreASTRepr])
    ast.left.map(println) // For debugging the failing tests
    ast.map { representation =>
      implicit val packageName = PackageName("foo")
      val componentsMap        = ASTTranslationFunctions.readComponentsToInterop(representation)(packageName)
      val routesMap            = ASTTranslationFunctions.readRoutesToInerop(representation)
      assert(
        componentsMap == Map(
          "Money" -> TypeRepr.NewTypeSymbol(
            valName = "Money",
            data = PrimitiveProduct(
              packageName = "foo",
              typeName = "Money",
              values = List(
                PrimitiveSymbol("value", PrimitiveNumber(Some(List()))),
                PrimitiveSymbol("unit", PrimitiveString(Some(List())))
              )
            )
          )
        )
      )
      assert(
        routesMap("_products") ==
          PathItemAggregation(
            path = "/products",
            items = List(
              PutOrPostRequest(
                path = "/products",
                `type` = POST,
                pathParams = List(),
                queries = Map("limit" -> PrimitiveOption(PrimitiveInt(None), None)),
//                queries = Map("limit" -> PrimitiveOption(PrimitiveInt(Some(List(Minimum(1), Maximum(5000)))), None)),
                request = Some(Ref("foo", "Money")),
                response = Some(Map("application/json" -> Ref("#/components/schemas/Money", "Money"))),
                hasReadOnlyType = Some(false),
                successStatusCode = 201
              ))
          )
      )
    }

    ast.isRight shouldBe true
  }

  "SwaggerAST" should "be translated properly for POST requests with simple string body" in {
    val yamlCode: String =
      """
      |paths:
      |  '/products':
      |    post:
      |      summary: Add a existing product to something
      |      requestBody:
      |        required: true
      |        content:
      |          application/text:
      |            schema:
      |              type: string
      |      responses:
      |        200:
      |          description: Product has successfully been added
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Money'
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
                  type: string
      |      required:
      |        - value
      |        - unit
    """.stripMargin

    val ast = parser.parse(yamlCode).flatMap(_.as[CoreASTRepr])
    ast.left.map(println) // For debugging the failing tests
    ast.map { representation =>
      implicit val packageName = PackageName("foo")
      val componentsMap        = ASTTranslationFunctions.readComponentsToInterop(representation)(packageName)
      val routesMap            = ASTTranslationFunctions.readRoutesToInerop(representation)
      assert(
        componentsMap == Map(
          "Money" -> TypeRepr.NewTypeSymbol(
            valName = "Money",
            data = PrimitiveProduct(
              packageName = "foo",
              typeName = "Money",
              values = List(
                PrimitiveSymbol("value", PrimitiveNumber(Some(List()))),
                PrimitiveSymbol("unit", PrimitiveString(Some(List())))
              )
            )
          )
        )
      )
      assert(
        routesMap("_products") ==
          PathItemAggregation(
            path = "/products",
            items = List(
              PutOrPostRequest(
                path = "/products",
                `type` = POST,
                pathParams = List(),
                queries = Map.empty,
                request = Some(PrimitiveString(Some(List()))),
                response = Some(Map("application/json" -> Ref("#/components/schemas/Money", "Money"))),
                hasReadOnlyType = Some(false),
                successStatusCode = 200
              ))
          )
      )
    }

    ast.isRight shouldBe true
  }

  it should "be able to read all yamls" in {
    val sources = List("catalog", "Contacts-API", "ERP-API", "problem", "purchasing", "registry").map(_ + ".yaml")
    sources.foreach { source =>
      info { source }
      val yaml: String                          = Source.fromResource(source).mkString
      val ast: Either[circe.Error, CoreASTRepr] = parser.parse(yaml).flatMap(_.as[CoreASTRepr])
      ast.left.map(x => println(x + s" in $source")) // For debugging the failing tests
      ast.isRight shouldBe true
    }
  }

  "readComponentsToInterop" should "be able to separate readOnly components from regular ones" in {
    val components = Map(
      "readOnlyComponent" -> SchemaObject(
        description = None,
        `type` = Some(SchemaObjectType.`object`),
        properties = Some(
          Map(
            "id" -> SchemaObject(
              description = None,
              `type` = Some(SchemaObjectType.string),
              items = None,
              readOnly = Some(true),
              minLength = None,
              maxLength = None
            ),
            "desc" -> SchemaObject(
              description = None,
              `type` = Some(SchemaObjectType.string),
              items = None,
              readOnly = None,
              minLength = None,
              maxLength = None
            )
          )
        ),
        enum = None,
        required = Some(List("id")),
        items = None
      ),
      "regularComponent" -> SchemaObject(
        description = None,
        `type` = Some(SchemaObjectType.`object`),
        properties = Some(
          Map(
            "id" -> SchemaObject(
              description = None,
              `type` = Some(SchemaObjectType.string),
              items = None,
              readOnly = None,
              minLength = None,
              maxLength = None
            )
          )
        ),
        enum = None,
        required = Some(List("id")),
        items = None
      )
    )
    val filtered: Map[String, SchemaObject] = ASTTranslationFunctions.splitReadOnlyComponents(components)

    filtered.size shouldBe 3
    filtered.keys should contain("readOnlyComponentRequest")
    filtered.get("readOnlyComponentRequest").map(_.properties.get.size) shouldBe Some(1)
    filtered.get("readOnlyComponentRequest").map(_.properties.get.keys) shouldBe Some(Set("desc"))
    filtered.get("regularComponent").map(_.properties.get.size) shouldBe Some(1)
    filtered.get("regularComponent") shouldBe components.get("regularComponent")
  }

}
