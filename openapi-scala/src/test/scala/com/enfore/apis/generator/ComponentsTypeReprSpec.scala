package com.enfore.apis.generator

import cats.data.NonEmptyList
import com.enfore.apis.repr.TypeRepr
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.meta._

class ComponentsTypeReprSpec extends AnyFlatSpec with Matchers {
  import TypeRepr._

  "ComponentsObject TypeRepr Generator" should "be able to generate a basic case class" in {
    val person: Symbol = NewTypeSymbol(
      "none",
      PrimitiveProduct(
        "com.enfore.apis",
        "Person",
        List(
          PrimitiveSymbol("name", PrimitiveString(None, None)),
          PrimitiveSymbol("age", PrimitiveInt(None, None))
        ),
        Some("Test Summary"),
        None
      )
    )
    val expected =
      """
        |package com.enfore.apis
        |
        |import io.circe._
        |import io.circe.generic.extras.Configuration
        |import io.circe.generic.extras.semiauto._
        |
        |/**
        |* Test Summary
        |**/
        |final case class Person(name: String, age: Int)
        |
        |object Person {
        | implicit val customConfig = Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
        | implicit val circeDecoder: Decoder[Person] = deriveDecoder[Person]
        | implicit val circeEncoder: Encoder[Person] = deriveEncoder[Person]
        |}
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(person).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "be able to generate a basic case class with no members" in {
    val person: Symbol = NewTypeSymbol(
      "none",
      PrimitiveProduct(
        "com.enfore.apis",
        "Person",
        List.empty,
        None,
        None
      )
    )
    val expected =
      """
        |package com.enfore.apis
        |
        |import io.circe._
        |import io.circe.generic.extras.Configuration
        |import io.circe.generic.extras.semiauto._
        |
        |final case class Person()
        |
        |object Person {
        | implicit val customConfig = Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
        | implicit val circeDecoder: Decoder[Person] = deriveDecoder[Person]
        | implicit val circeEncoder: Encoder[Person] = deriveEncoder[Person]
        |}
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(person).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "be able to resolve references" in {
    val person: Symbol = NewTypeSymbol(
      "none",
      PrimitiveProduct(
        "com.enfore.apis",
        "Person",
        List(
          RefSymbol("name", Ref("com.enfore.apis", "Name", None, None, None)),
          PrimitiveSymbol("age", PrimitiveInt(None, None))
        ),
        None,
        None
      )
    )
    val expected =
      """
        |package com.enfore.apis
        |
        |import io.circe._
        |import io.circe.generic.extras.Configuration
        |import io.circe.generic.extras.semiauto._
        |
        |final case class Person(name: com.enfore.apis.Name, age: Int)
        |
        |object Person {
        | implicit val customConfig = Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
        | implicit val circeDecoder: Decoder[Person] = deriveDecoder[Person]
        | implicit val circeEncoder: Encoder[Person] = deriveEncoder[Person]
        |}
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(person).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "be able to generate enums" in {
    val animal: Symbol = NewTypeSymbol(
      "none",
      PrimitiveEnum("com.enfore.apis", "Animal", Set("Human", "Dolphin", "Dog"), None, None)
    )
    val expected =
      """
        |package com.enfore.apis
        |
        |import enumeratum._
        |
        |sealed trait Animal extends EnumEntry
        |object Animal extends Enum[Animal] with CirceEnum[Animal] {
        | val values = findValues
        | case object Human extends Animal
        | case object Dolphin extends Animal
        | case object Dog extends Animal
        |}
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(animal).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "be able to resolve references with default values" in {
    val person: Symbol = NewTypeSymbol(
      "none",
      PrimitiveProduct(
        "com.enfore.apis",
        "Person",
        List(
          RefSymbol("name", Ref("com.enfore.apis", "Name", Some("MAGIC"), None, None)),
          PrimitiveSymbol("age", PrimitiveInt(None, None))
        ),
        None,
        None
      )
    )
    val expected =
      """
        |package com.enfore.apis
        |
        |import io.circe._
        |import io.circe.generic.extras.Configuration
        |import io.circe.generic.extras.semiauto._
        |
        |final case class Person(name: com.enfore.apis.Name = com.enfore.apis.Name.MAGIC, age: Int)
        |
        |object Person {
        | implicit val customConfig = Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
        | implicit val circeDecoder: Decoder[Person] = deriveDecoder[Person]
        | implicit val circeEncoder: Encoder[Person] = deriveEncoder[Person]
        |}
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(person).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "be able to handle type parameters" in {
    val paramedType: Symbol = NewTypeSymbol(
      "null",
      PrimitiveProduct(
        "com.enfore.apis",
        "ParamedType",
        List(
          PrimitiveSymbol("optionalVal", PrimitiveOption(PrimitiveString(None, None))),
          PrimitiveSymbol("listVal", PrimitiveArray(PrimitiveInt(None, None), None)),
          PrimitiveSymbol("dictVal", PrimitiveDict(PrimitiveInt(None, None), None)),
          PrimitiveSymbol("opListVal", PrimitiveOption(PrimitiveArray(PrimitiveBoolean(None, None), None))),
          PrimitiveSymbol("listOpVal", PrimitiveArray(PrimitiveOption(PrimitiveNumber(None, None)), None))
        ),
        None,
        None
      )
    )

    val expected: Parsed[Source] =
      """
        |package com.enfore.apis
        |
        |import io.circe._
        |import io.circe.generic.extras.Configuration
        |import io.circe.generic.extras.semiauto._
        |
        |final case class ParamedType(optionalVal: Option[String],
        | listVal: List[Int],
        | dictVal: Map[String, Int],
        | opListVal: Option[List[Boolean]],
        | listOpVal: List[Option[Double]])
        |
        | object ParamedType {
        |   implicit val customConfig = Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
        |   implicit val circeDecoder: Decoder[ParamedType] = deriveDecoder[ParamedType]
        |   implicit val circeEncoder: Encoder[ParamedType] = deriveEncoder[ParamedType]
        | }
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(paramedType).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "deal with type refinements properly in strings" in {
    val refinedType: Symbol = NewTypeSymbol(
      "null",
      PrimitiveProduct(
        "com.enfore.apis",
        "RefinedType",
        List(
          PrimitiveSymbol("stringVal", PrimitiveString(Some(NonEmptyList.of(MinLength(3), MaxLength(3))), None))
        ),
        None,
        None
      )
    )

    val expected =
      """
        |package com.enfore.apis
        |
        |import io.circe._
        |import io.circe.generic.extras.Configuration
        |import io.circe.generic.extras.semiauto._
        |
        |import eu.timepit.refined._
        |import eu.timepit.refined.api._
        |import eu.timepit.refined.collection._
        |import eu.timepit.refined.numeric._
        |import shapeless._
        |import eu.timepit.refined.boolean._
        |import io.circe.refined._
        |
        |final case class RefinedType(stringVal : String Refined AllOf[MinSize[W.`3`.T] :: MaxSize[W.`3`.T] :: HNil])
        |
        |object RefinedType {
        | implicit val customConfig = Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
        | implicit val circeDecoder: Decoder[RefinedType] = deriveDecoder[RefinedType]
        | implicit val circeEncoder: Encoder[RefinedType] = deriveEncoder[RefinedType]
        | object RefinementConstructors {
        |   val stringVal = new RefinedTypeOps[String Refined AllOf[MinSize[W.`3`.T] :: MaxSize[W.`3`.T] :: HNil], String]
        | }
        | type `stringValRefined` = String Refined AllOf[MinSize[W.`3`.T] :: MaxSize[W.`3`.T] :: HNil]
        |}
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(refinedType).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "deal with type refinements in numbers properly" in {
    val refinedType: Symbol = NewTypeSymbol(
      "null",
      PrimitiveProduct(
        "com.enfore.apis",
        "RefinedType",
        List(
          PrimitiveSymbol("intVal", PrimitiveInt(Some(NonEmptyList.of(Minimum(10), Maximum(15))), None))
        ),
        None,
        None
      )
    )
    val expected =
      """package com.enfore.apis
                     |
                     |import io.circe._
                     |import io.circe.derivation._
                     |
                     |
                     |import eu.timepit.refined._
                     |import eu.timepit.refined.api._
                     |import eu.timepit.refined.collection._
                     |import eu.timepit.refined.numeric._
                     |import shapeless._
                     |import eu.timepit.refined.boolean._
                     |import io.circe.refined._
                     |
                     |
                     |
                     |final case class RefinedType(
                     |	intVal : Int Refined AllOf[GreaterEqual[10] :: LessEqual[15] :: HNil]
                     |) 
                     |
                     |object RefinedType {
                     |	implicit val circeDecoder: Decoder[RefinedType] = deriveDecoder[RefinedType]
                     |	implicit val circeEncoder: Encoder[RefinedType] = deriveEncoder[RefinedType]
                     |
                     |object RefinementConstructors {
                     |	val intVal = new RefinedTypeOps[Int Refined AllOf[GreaterEqual[10] :: LessEqual[15] :: HNil], Int]
                     |	}
                     |type `intValRefined` = Int Refined AllOf[GreaterEqual[10] :: LessEqual[15] :: HNil]
                     |
                     |}""".stripMargin

    val tree =
      ScalaGenerator.codeGenerator.generateScala(refinedType).split("\n").map(_.trim).filter(_.isEmpty).mkString("\n")
    tree shouldBe expected.split("\n").map(_.trim).filter(_.isEmpty).mkString("\n")
  }

  it should "deal with type refinements with type parameters" in {
    val refinedType: Symbol = NewTypeSymbol(
      "null",
      PrimitiveProduct(
        "com.enfore.apis",
        "RefinedType",
        List(
          PrimitiveSymbol(
            "listString",
            PrimitiveArray(PrimitiveString(None, None), Some(NonEmptyList.of(MinLength(3), MaxLength(3))))
          )
        ),
        None,
        None
      )
    )
    val expected =
      """
        |package com.enfore.apis
        |
        |import io.circe._
        |import io.circe.generic.extras.Configuration
        |import io.circe.generic.extras.semiauto._
        |
        |import eu.timepit.refined._
        |import eu.timepit.refined.api._
        |import eu.timepit.refined.collection._
        |import eu.timepit.refined.numeric._
        |import shapeless._
        |import eu.timepit.refined.boolean._
        |import io.circe.refined._
        |
        |final case class RefinedType(listString : List[String] Refined AllOf[MinSize[W.`3`.T] :: MaxSize[W.`3`.T] :: HNil])
        |
        |object RefinedType {
        | implicit val customConfig = Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
        | implicit val circeDecoder: Decoder[RefinedType] = deriveDecoder[RefinedType]
        | implicit val circeEncoder: Encoder[RefinedType] = deriveEncoder[RefinedType]
        | object RefinementConstructors {
        |   val listString = new RefinedTypeOps[List[String] Refined AllOf[MinSize[W.`3`.T] :: MaxSize[W.`3`.T] :: HNil], List[String]]
        | }
        | type `listStringRefined` = List[String] Refined AllOf[MinSize[W.`3`.T] :: MaxSize[W.`3`.T] :: HNil]
        |}
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(refinedType).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "deal with refinements inside of other type parameters" in {
    val refinedType: Symbol = NewTypeSymbol(
      "null",
      PrimitiveProduct(
        "com.enfore.apis",
        "RefinedType",
        List(
          PrimitiveSymbol(
            "nested",
            PrimitiveOption(PrimitiveString(Some(NonEmptyList.of(MinLength(2), MaxLength(2))), None))
          )
        ),
        None,
        None
      )
    )

    val expected =
      """
        |package com.enfore.apis
        |
        |import io.circe._
        |import io.circe.generic.extras.Configuration
        |import io.circe.generic.extras.semiauto._
        |
        |
        |import eu.timepit.refined._
        |import eu.timepit.refined.api._
        |import eu.timepit.refined.collection._
        |import eu.timepit.refined.numeric._
        |import shapeless._
        |import eu.timepit.refined.boolean._
        |import io.circe.refined._
        |
        |final case class RefinedType(nested : Option[String Refined AllOf[MinSize[W.`2`.T] :: MaxSize[W.`2`.T] :: HNil]])
        |
        | object RefinedType {
        |  implicit val customConfig = Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
        |  implicit val circeDecoder: Decoder[RefinedType] = deriveDecoder[RefinedType]
        |  implicit val circeEncoder: Encoder[RefinedType] = deriveEncoder[RefinedType]
        |
        |
        |  object RefinementConstructors {
        |        val nested = new RefinedTypeOps[String Refined AllOf[MinSize[W.`2`.T] :: MaxSize[W.`2`.T] :: HNil], String]
        |        }
        |
        |  type `nestedRefined` = String Refined AllOf[MinSize[W.`2`.T] :: MaxSize[W.`2`.T] :: HNil]
        |
        | }
        |
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(refinedType).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "be able to work with union types" in {
    val unionTypeSymbol: Symbol = NewTypeSymbol(
      "Customer",
      PrimitiveUnion(
        "com.enfore.test",
        "Customer",
        Set(
          Ref("#.components.schemas", "IndividualCustomer", None, None, None),
          Ref("#.components.schemas", "OrganizationCustomer", None, None, None)
        ),
        "@type",
        None,
        None
      )
    )

    val expected =
      """
        |package com.enfore.test
        |
        |import shapeless._
        |import io.circe._
        |import io.circe.syntax._
        |import Customer._
        |
        |final case class Customer(value: Union)
        |
        |object Customer {
        | type Union = IndividualCustomer :+: OrganizationCustomer :+: CNil
        |
        | object jsonConversions extends Poly1 {
        |   implicit def caseIndividualCustomer = at[IndividualCustomer](_.asJson.dropNullValues.deepMerge(Json.obj("@type" -> Json.fromString("IndividualCustomer"))))
        |   implicit def caseOrganizationCustomer = at[OrganizationCustomer](_.asJson.dropNullValues.deepMerge(Json.obj("@type" -> Json.fromString("OrganizationCustomer"))))
        | }
        |
        | implicit val customEncoders: Encoder[Customer] = new Encoder[Customer] {
        |   def apply(a: Customer): Json = {
        |     (a.value map jsonConversions).unify
        |   }
        | }
        |
        | implicit val customDecoder: Decoder[Customer] = new Decoder[Customer] {
        |   def apply(c: HCursor): Decoder.Result[Customer] = {
        |     val output = c.downField("@type").as[String] match {
        |       case Right("IndividualCustomer") => c.value.as[IndividualCustomer].map(Coproduct[Union](_))
        |       case Right("OrganizationCustomer") => c.value.as[OrganizationCustomer].map(Coproduct[Union](_))
        |       case _ => Left(DecodingFailure.apply("Type information not available", List(CursorOp.DownField("@type"))))
        |     }
        |     output.map(Customer.apply)
        |   }
        | }
        |}
        |""".stripMargin.trim.parse[Source]

    val tree: Parsed[Source] = ScalaGenerator.codeGenerator.generateScala(unionTypeSymbol).parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

}
