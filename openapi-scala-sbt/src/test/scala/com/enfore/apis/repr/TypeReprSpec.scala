package com.enfore.apis.repr

import com.enfore.apis.generator.ScalaGenerator
import ScalaGenerator.ops._
import scala.meta._
import org.scalatest._

class TypeReprSpec extends FlatSpec with Matchers {
  import TypeRepr._

  "TypeReprGen" should "be able to generate a basic case class" in {
    val person: Symbol = NewTypeSymbol(
      "none",
      PrimitiveProduct(
        "com.enfore.apis",
        "Person",
        List(
          PrimitiveSymbol("name", PrimitiveString),
          PrimitiveSymbol("age", PrimitiveInt),
        ))
    )
    val expected =
      """
        |package com.enfore.apis
        |
        |final case class Person(name: String, age: Int)
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = person.generateScala.parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "be able to generate enums" in {
    val animal: Symbol = NewTypeSymbol(
      "none",
      PrimitiveEnum("com.enfore.apis", "Animal", Set("Human", "Dolphin", "Dog"))
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
    val tree: Parsed[Source] = animal.generateScala.parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

  it should "be able to handle type parameters" in {
    val paramedType: Symbol = NewTypeSymbol(
      "null",
      PrimitiveProduct(
        "com.enfore.apis",
        "ParamedType",
        List(
          PrimitiveSymbol("optionalVal", PrimitiveOption(PrimitiveString)),
          PrimitiveSymbol("listVal", PrimitiveArray(PrimitiveInt)),
          PrimitiveSymbol("opListVal", PrimitiveOption(PrimitiveArray(PrimitiveBoolean))),
          PrimitiveSymbol("listOpVal", PrimitiveArray(PrimitiveOption(PrimitiveNumber)))
        )
      )
    )
    val expected: Parsed[Source] =
      """
        |package com.enfore.apis
        |
        |final case class ParamedType(optionalVal: Option[String],
        | listVal: List[Int],
        | opListVal: Option[List[Boolean]],
        | listOpVal: List[Option[Double]])
      """.stripMargin.trim.parse[Source]
    val tree: Parsed[Source] = paramedType.generateScala.parse[Source]
    tree.get.structure shouldBe expected.get.structure
  }

}
