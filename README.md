# OpenAPI Scala

This project contains an opinionated library and SBT Plugin for Scala code generation from [OpenAPI 3.0](https://swagger.io/specification/) compliant YAML. 

This will generate Components (i.e., schemas or types), and Paths (i.e., route definitions) for REST APIs.

We do not support all OpenAPI 3.0 features. For more details on what is supported, look at the [Limitations](#limitations) section.

### OpenAPI Scala

SBT sub-project `openapi-scala` contains the main logic and library for loading, representing, and translating YAML to Scala code.
These contain components that are translated into Scala's `case classes` and Routes, which are explained in the next section.

#### Routes

We support different types of Route generation, depending on the backend you need it for. 

- The generic Routes are each translated to a Scala `trait` declaring interfaces for that particular HTTP route.
- The http4s Routes translate into two files, one file with a trait for the implementation you'll need to provide and one file with an object apply function of which accepts mentioned implementation trait. 

The names of the functions and values for the routes is generated at run-time. However, if an `operationId` field is defined (as specified in OAS 3.0) we will use those to generate user-friendly function names.

## Components

Components in OpenAPI are the types that can be referred to as inputs/outputs for routes. A common use of components is to define product types. We take these components and translate them to Scala case classes. Consider the following example:

```yaml
components:
    schemas:
        Person:
            description: My test description
            properties:
                name:
                    type: string
                weight:
                    type: number
```

The code above will be translated to following Scala code:

```scala
/**
 * My test description
**/
final case class Person(name: String, weight: Double)

object Person {
    implicit val customDecoders = deriveDecoder[Person](renaming.snakeCase, None)
    implicit val customEncoders = deriveEncoder[Person](renaming.snakeCase, true, None)

}
```

### sbt-openapi

SBT sub-project `sbt-openapi` contains an SBT plugin that allows the use of `openapi-scala` library to a given YAML file that will be loaded
and used to generate managed Scala sources.

## Quickstart

A release log of the plugin and the library can be found [here](https://github.numberfour.eu/Server/openapi-scala/releases).

To use the plugin, you first need to make it available in your SBT.

```scala
// project/plugins.sbt
addSbtPlugin("com.enfore" % "sbt-openapi" % "<openapi-scala-version>")
```

You will also need to make the library available to be able to use the generated code.

```scala
libraryDependencies += "com.enfore" %% "openapi-lib" % "<openapi-scala-version>"
```

Additionally, you will need to satisfy these library dependencies for `openapi-lib`. The provided versions are tested and made sure to work. You should be able to use any version compatible with your project though.

```scala
"com.beachape" %% "enumeratum"       % "1.5.13",
"com.beachape" %% "enumeratum-circe" % "1.5.20",
"io.circe"     %% "circe-derivation" % "0.11.0-M1",
"com.chuusai"  %% "shapeless"        % "2.3.3"
```

For http4s Routes you will also need: 

```scala
"org.http4s" %% s"http4s-dsl" % "0.20.0-M7",
"org.http4s" %% s"http4s-circe" % "0.20.0-M7"
```

Once the plugin is available in your project you can enable it on a given an SBT sub-project and use the setting `openAPIOutputPackage` to specify
the package name for your components. 

```scala
// build.sbt
lazy val root = (project in ("."))
    .settings(
        openAPIOutputPackage := "com.enfore.model",
        libraryDependencies += "com.enfore" %% "openapi-lib" % "<openapi-scala-version>"
    )
    .enablePlugins(OpenapiPlugin)
```

### SBT Settings

Following are the settings available for your SBT project.

**openAPISource**: Source directory for OpenAPI. Defaults to `src/main/openapi` of the project.

**openAPIOutput**: Output directory for the OpenAPI. Defaults to managed sources — `openapi`.

**openAPIOutputPacakge**: Name of the package to be used for OpenAPI components.

**routeImplementations**: A List of `com.enfore.apis.generator.RouteImplementation`, which controls which kind of routes should be generated. Find out more about [routes](#Routes)

## Refinements

OAS 3.0 allows one to set constraints on the primitive input types. We use the [Refined](https://github.com/fthomas/refined) library for Scala to reproduce these in the generated code. This means that you do not need to manually setup validations for any refinements and constraints in the inputs for your API. This, however, also means that you will have to return values with refinements if there are any in the return types.

In the example code below, we also generate a constructor for a value over which refinements exist. These refinements exist inside an object called `RefinementConstructors` that is further nested in the companion object for the generated case class. The names of the constructors will be the same as the values for which the refinements exist.

```scala
import shapeless._

final case class MyTestOutput(name: String Refined AllOf[MinSize[W.`1`.T] :: MaxSize[W.`256`.T] :: HNil])

object MyTestOutput {
    object RefinementConstructors {
        val name = String Refined AllOf[MinSize[W.`1`.T] :: MaxSize[W.`256`.T] :: HNil]

    }
}
```

## Read Only Properties

OpenAPI 3.0 supports marking properties in objects as read-only. This checks that the property is available if the type is used as the output of a route, but not for inputs.

We model this in Scala by generating two types in case a field is marked read-only. The generator will then automatically use the right type depending on weather the object is used as input or output.

```yaml
MyObject:
    type: object
    properties:
        id:
            type: string
            readOnly: true
        body:
            type: string
```

This will generate two Scala classes that are as follows:

```scala
final case class MyObjectRequest(body: String)

final case class MyObject(id: String, body: String)
```

## Unions & Enums

OpenAPI 3.0 uses `oneOf` types to represent unions or enumerations. In Scala, there is no way to have proper Algebraic Data Types (ADTs). While products can be represented using case classes, the only way to represent unions is to create classes that extend a given trait.

This, however, makes defining arbitrary unions very difficult. Therefore, we use [Shapeless' discriminated unions](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#coproducts-and-discriminated-unions) to represent `anyOf` types in Scala.

```scala
final case class MyUnionWrapper(value: Union)

object MyUnionWrapper {
    type Union = Item1 :+: Item2 :+: CNil

    object jsonConversions extends Poly1 {
        implicit def caseItem1 = at[SingleTargetConversion](_.asJson.deepMerge(Json.obj("@type" -> Json.fromString("Item1"))))
        implicit def caseItem2 = at[MultiTargetConversion](_.asJson.deepMerge(Json.obj("@type" -> Json.fromString("Item2"))))

    }

    implicit val customerEncoders = new Encoder[MyUnionWrapper] {
      def apply(a: Conversion): Json = {
        (a.value map jsonConversions).unify
      }
    }

  implicit val customDecoder: Decoder[MyUnionWrapper] = new Decoder[Conversion] {
    def apply(c: HCursor): Decoder.Result[MyUnionWrapper] = {
      val output = c.downField("@type").as[String] match {
        case Right("Item1") => c.value.as[Item1].map(Coproduct[Union](_))
        case Right("Item2") => c.value.as[Item2].map(Coproduct[Union](_))
        case _ => Left(DecodingFailure.apply("Type information not available", List(CursorOp.DownField("@type"))))
      }
      output.map(MyUnionWrapper.apply)
    }
  }

}
```

Using these arbitrary unions is easier than it first appears. To create a value of type `MyUnionWrapper`, which is defined using `oneOf` in OpenAPI, we can use the following code:

```scala
import shapeless._
val item1Union = MyUnionWrapper(Coproduct[MyUnionWrapper.Union](item1))
val item2Union = MyUnionWrapper(Coproduct[MyUnionWrapper.Union](item2))
```

Handling the values of that are already wrapped and mapping them to something is slightly more work. We use the `Poly1`  from Shapeless to create functions that can map over a co-product.

```scala
object convertToT extends Poly1 {
    implicit val oneToT = at[Item1](x => oneToT(x))
    implicit val twoToT = at[Item2](x => twoToT(x))
}

val convertedItem: T = item1Union.fold(convertToT)
```

Our `convertToT` function converts every possible value to a type `T` and then we unify them. Since we know that our co-product will contain exactly one value of the possible given types, we can be certain that we will end up with at least one value of type `T`.

## Limitations

This generator is highly opinionated and skips over certain features defined OpenAPI 3.0 specification for the sake of API consistency and compatibility with modern typed languages.

### Limitations

- We do not support any anonymous types for representing HTTP requests and responses.
- We do not support nested type definitions. Therefore, any objects that are used must be defined in the components and must not contain
  any other nested objects. Nested arrays, however, are supported.
- We do not support more than one type for Content-Type header for incoming requests. Multiple types corresponding to their encodings are, however, available with an Accept-Encoding header on a request.
- We do not support `anyOf` types from OpenAPI 3.0. Suggestions as to how the behaviour for the same should be represented in generated Scala code are welcome.
- We do not support inlined schema definition for input and output object types in route definitions in OpenAPI input files. Therefore, these must be defined as components and referred.
- We do not support type aliasing or rich typing. For instance, if you have a primitive type with a particular set of refinements, you would have to point out those refinements everywhere with those refinements instead of referring them from a definition.
