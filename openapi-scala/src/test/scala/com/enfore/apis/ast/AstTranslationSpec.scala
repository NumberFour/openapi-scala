package com.enfore.apis.ast

import cats.data.NonEmptyList
import cats.syntax.functor._
import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.ast.SwaggerAST._
import com.enfore.apis.repr.ReqWithContentType.{PATCH, POST}
import com.enfore.apis.repr.TypeRepr.{
  Maximum,
  Minimum,
  NewTypeSymbol,
  PrimitiveEnum,
  PrimitiveInt,
  PrimitiveNumber,
  PrimitiveOption,
  PrimitiveProduct,
  PrimitiveString,
  PrimitiveSymbol,
  PrimitiveUnion,
  Ref,
  RefSymbol
}
import com.enfore.apis.repr.{PathItemAggregation, RequestWithPayload, TypeRepr}
import io.circe
import io.circe._
import io.circe.generic.auto._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class AstTranslationSpec extends AnyFlatSpec with Matchers {

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
      |      operationId: dummyFunction
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
      |          type: string
      |          default: EUR
      |      required:
      |        - value
    """.stripMargin

    val ast = circe.yaml.parser.parse(yamlCode).flatMap(_.as[CoreASTRepr])
    ast.left.map(println) // For debugging the failing tests
    ast.map { representation =>
      implicit val packageName = PackageName("foo")
      val componentsMap        = ASTTranslationFunctions.readComponentsToInterop(representation)(packageName)
      val routesMap            = ASTTranslationFunctions.readRoutesToInterop(representation)

      assert(
        componentsMap == Map(
          "Money" -> TypeRepr.NewTypeSymbol(
            valName = "Money",
            data = PrimitiveProduct(
              packageName = "foo",
              typeName = "Money",
              values = List(
                PrimitiveSymbol("value", PrimitiveNumber(None, None)),
                PrimitiveSymbol(
                  "unit",
                  PrimitiveString(None, Some("EUR"))
                )
              ),
              summary = None,
              description = Some(
                """A Money represents a monetary value (i.e., a currency and an amount), for example "120 EUR" or "2,500.75 USD"."""
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
              RequestWithPayload(
                path = "/products",
                summary = Some("Add a new product to the organisation"),
                description = None,
                operationId = Some("dummyFunction"),
                `type` = POST,
                pathParams = List(),
                queries =
                  Map("limit" -> PrimitiveOption(PrimitiveInt(Some(NonEmptyList.of(Minimum(1), Maximum(5000))), None))),
                request = Some(Ref("foo", "Money", None)),
                response = Some(Map("application/json" -> Ref("#/components/schemas/Money", "Money", None))),
                hasReadOnlyType = Some(false),
                successStatusCode = 201
              )
            )
          )
      )
    }

    ast.isRight shouldBe true
  }

  it should "be able to translate the default values for integers properly" in {
    val yamlCode: String =
      """
        |components:
        | schemas:
        |   ContainsDefault:
        |     type: object
        |     properties:
        |       something:
        |         type: integer
        |         default: 10
        |""".stripMargin
    val expectedComp: Map[String, NewTypeSymbol] = Map(
      "ContainsDefault" -> NewTypeSymbol(
        valName = "ContainsDefault",
        data = PrimitiveProduct(
          packageName = "foo",
          typeName = "ContainsDefault",
          values = List(
            PrimitiveSymbol(
              "something",
              PrimitiveInt(None, Some(10))
            )
          ),
          summary = None,
          description = None
        )
      )
    )
    parseYamlAndCompare(yamlCode, expectedComp)
  }

  private def parseYamlAndCompare(yamlCode: String, expectedComp: Map[String, TypeRepr.NewTypeSymbol]) = {
    val ast = circe.yaml.parser.parse(yamlCode).flatMap(_.as[CoreASTRepr])
    ast.left.map(println)
    ast
      .map { representation =>
        implicit val packageName: PackageName = PackageName("foo")
        val componentsMap =
          ASTTranslationFunctions.readComponentsToInterop(representation)(packageName)
        componentsMap shouldBe expectedComp
      }
      .getOrElse(throw new Exception("Could not parse yamlCode"))
  }

  it should "be able to translate the default values for numbers properly" in {
    val yamlCode: String =
      """
        |components:
        | schemas:
        |   ContainsDefault:
        |     type: object
        |     properties:
        |       something:
        |         type: number
        |         default: 10.2
        |""".stripMargin
    val expectedComp: Map[String, NewTypeSymbol] = Map(
      "ContainsDefault" -> NewTypeSymbol(
        valName = "ContainsDefault",
        data = PrimitiveProduct(
          packageName = "foo",
          typeName = "ContainsDefault",
          values = List(
            PrimitiveSymbol(
              "something",
              PrimitiveNumber(None, Some(10.2))
            )
          ),
          summary = None,
          description = None
        )
      )
    )
    parseYamlAndCompare(yamlCode, expectedComp)
  }

  it should "be able to translate the default values for strings properly" in {
    val yamlCode: String =
      """
        |components:
        | schemas:
        |   ContainsDefault:
        |     type: object
        |     properties:
        |       something:
        |         type: string
        |         default: foo
        |""".stripMargin
    val expectedComp: Map[String, NewTypeSymbol] = Map(
      "ContainsDefault" -> NewTypeSymbol(
        valName = "ContainsDefault",
        data = PrimitiveProduct(
          packageName = "foo",
          typeName = "ContainsDefault",
          values = List(
            PrimitiveSymbol(
              "something",
              PrimitiveString(None, Some("foo"))
            )
          ),
          summary = None,
          description = None
        )
      )
    )
    parseYamlAndCompare(yamlCode, expectedComp)
  }

  it should "be able to translate the default values for enums properly" in {
    val yamlCode: String =
      """
        |components:
        |  schemas:
        |    ContainsDefault:
        |      type: object
        |      properties:
        |        something:
        |          $ref: '#/components/schemas/SomeType'
        |    SomeType:
        |      type: string
        |      enum:
        |        - NONE
        |        - SOME
        |      default: NONE
        |""".stripMargin
    val expectedComp: Map[String, NewTypeSymbol] = Map(
      "ContainsDefault" -> NewTypeSymbol(
        valName = "ContainsDefault",
        data = PrimitiveProduct(
          packageName = "foo",
          typeName = "ContainsDefault",
          values = List(
            RefSymbol("something", Ref("foo", "SomeType", Some("NONE")))
          ),
          summary = None,
          description = None
        )
      ),
      "SomeType" -> NewTypeSymbol(
        "SomeType",
        PrimitiveEnum("foo", "SomeType", Set("NONE", "SOME"), None, None)
      )
    )
    parseYamlAndCompare(yamlCode, expectedComp)
  }

  it should "fail to translate a float default value for an integer type" in {
    val yamlCode: String =
      """
        |components:
        | schemas:
        |   ContainsDefault:
        |     type: object
        |     properties:
        |       something:
        |         type: integer
        |         default: 10.2
        |""".stripMargin
    val ast = circe.yaml.parser.parse(yamlCode).flatMap(_.as[CoreASTRepr])
    ast.left.map(println) // For debugging the failing tests
    ast.map { representation =>
      implicit val packageName: PackageName = PackageName("foo")
      an[AssertionError] shouldBe thrownBy(ASTTranslationFunctions.readComponentsToInterop(representation)(packageName))
    }

  }

  it should "be able to read the sum types properly" in {
    val yamlCode: String =
      """
        |components:
        | schemas:
        |   IndividualCustomer:
        |     type: object
        |     properties:
        |       name:
        |         description: Name of the person
        |         type: string
        |       age:
        |         description: Age of the person
        |         type: number
        |     required:
        |       - name
        |       - age
        |   OrganizationCustomer:
        |     type: object
        |     properties:
        |       name:
        |         description: Name of the person
        |         type: string
        |       orgName:
        |         description: Name of the org
        |         type: string
        |     required:
        |       - name
        |       - orgName
        |   Customer:
        |     discriminator:
        |       propertyName: '@type'
        |     oneOf:
        |       - $ref: '#/components/schemas/IndividualCustomer'
        |       - $ref: '#/components/schemas/OrganizationCustomer'
        |""".stripMargin

    val ast: Either[Error, CoreASTRepr] = circe.yaml.parser.parse(yamlCode).flatMap(_.as[CoreASTRepr])
    ast.left.map(println)
    ast.isRight shouldBe true
    val components: Map[String, SchemaObject] = ast.right.get.components.schemas
    components("Customer").oneOf.isDefined shouldBe true
    val interop: Map[String, TypeRepr.Symbol] =
      ASTTranslationFunctions.readComponentsToInterop(ast.right.get)(PackageName("com.enfore.test"))
    interop("Customer").isInstanceOf[NewTypeSymbol] shouldBe true
    interop("Customer").asInstanceOf[NewTypeSymbol].data.isInstanceOf[PrimitiveUnion] shouldBe true
  }

  it should "be translated properly for POST requests with simple string body" in {
    val yamlCode: String =
      """
      |paths:
      |  '/products':
      |    post:
      |      summary: Add a existing product to something
      |      operationId: dummyFunction
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
      |          type: string
      |      required:
      |        - value
      |        - unit
    """.stripMargin

    val ast = circe.yaml.parser.parse(yamlCode).flatMap(_.as[CoreASTRepr])
    ast.left.map(println) // For debugging the failing tests
    ast.map { representation =>
      implicit val packageName = PackageName("foo")
      val componentsMap        = ASTTranslationFunctions.readComponentsToInterop(representation)(packageName)
      val routesMap            = ASTTranslationFunctions.readRoutesToInterop(representation)
      assert(
        componentsMap == Map(
          "Money" -> TypeRepr.NewTypeSymbol(
            valName = "Money",
            data = PrimitiveProduct(
              packageName = "foo",
              typeName = "Money",
              values = List(
                PrimitiveSymbol("value", PrimitiveNumber(None, None)),
                PrimitiveSymbol("unit", PrimitiveString(None, None))
              ),
              summary = None,
              description = Some(
                """A Money represents a monetary value (i.e., a currency and an amount), for example "120 EUR" or "2,500.75 USD"."""
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
              RequestWithPayload(
                path = "/products",
                summary = Some("Add a existing product to something"),
                description = None,
                operationId = Some("dummyFunction"),
                `type` = POST,
                pathParams = List(),
                queries = Map.empty,
                request = Some(PrimitiveString(None, None)),
                response = Some(Map("application/json" -> Ref("#/components/schemas/Money", "Money", None))),
                hasReadOnlyType = Some(false),
                successStatusCode = 200
              )
            )
          )
      )
    }

    ast.isRight shouldBe true
  }

  it should "be translated properly for PATCH requests" in {
    val yamlCode: String =
      """
      |paths:
      |  '/products':
      |    patch:
      |      summary: Partially update an existing product
      |      operationId: dummyFunction
      |      requestBody:
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Money'
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
      |          type: string
      |      required:
      |        - value
      |        - unit
    """.stripMargin

    val ast = circe.yaml.parser.parse(yamlCode).flatMap(_.as[CoreASTRepr])
    ast.left.map(println) // For debugging the failing tests
    ast.map { representation =>
      implicit val packageName = PackageName("foo")
      val componentsMap        = ASTTranslationFunctions.readComponentsToInterop(representation)(packageName)
      val routesMap            = ASTTranslationFunctions.readRoutesToInterop(representation)
      assert(
        componentsMap == Map(
          "Money" -> TypeRepr.NewTypeSymbol(
            valName = "Money",
            data = PrimitiveProduct(
              packageName = "foo",
              typeName = "Money",
              values = List(
                PrimitiveSymbol("value", PrimitiveNumber(None, None)),
                PrimitiveSymbol("unit", PrimitiveString(None, None))
              ),
              summary = None,
              description = Some(
                """A Money represents a monetary value (i.e., a currency and an amount), for example "120 EUR" or "2,500.75 USD"."""
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
              RequestWithPayload(
                path = "/products",
                summary = Some("Partially update an existing product"),
                description = None,
                operationId = Some("dummyFunction"),
                `type` = PATCH,
                pathParams = List(),
                queries = Map.empty,
                request = Some(Ref("foo", "Money", None)),
                response = Some(Map("application/json" -> Ref("#/components/schemas/Money", "Money", None))),
                hasReadOnlyType = Some(false),
                successStatusCode = 200
              )
            )
          )
      )
    }

    ast.isRight shouldBe true
  }

  it should "be able to read all yamls" in {
    val sources = List("catalog", "Contacts-API", "ERP-API", "problem", "purchasing", "registry").map(_ + ".json")
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

  "Helper functions" should "ensure that a warning is produced on camelCase strings" in {
    val expr = ASTTranslationFunctions.camelCaseExpr

    expr.findAllIn("myCamelCase").toList shouldBe List("yC", "lC")
    expr.findAllIn("SimpleCase").toList shouldBe List("eC")
    expr.findAllIn("myMix_case_Expr").toList shouldBe List("yM")
    expr.findAllIn("my_snake_case").toList shouldBe List()
  }

}
