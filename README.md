# OpenAPI Scala

This project contains an opinionated library and SBT Plugin for Scala code generation from [OpenAPI 3.1](https://swagger.io/specification/) compliant YAML. 

This will generate data structures(i.e., schemas or types), Circe JSON serializers, and Paths (i.e., route definitions currently targeting HTTP4s) for REST APIs.

We do not support all OpenAPI 3.1 features. For more details on what is supported, look at the [Limitations](#limitations) section.

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

// TODO shouldn't we automatically add these dependencies via the SBT auto plugin via flag?
```scala
"com.beachape" %% "enumeratum"       % "1.5.13",
"com.beachape" %% "enumeratum-circe" % "1.5.20",
"io.circe"     %% "circe-derivation" % "0.11.0-M1",
"com.chuusai"  %% "shapeless"        % "2.3.3"
```

For http4s Routes you will also need: 

```scala
"org.http4s" %% s"http4s-dsl" % "0.21.7",
"org.http4s" %% s"http4s-circe" % "0.21.7"
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

## Technical details: Modules

![https://mermaid-js.github.io/mermaid-live-editor/#/edit/eyJjb2RlIjoiZ3JhcGggVERcbkFbWWFtbCBmaWxlc10gLS0-fENvbWJpbmVkIHdpdGggc2J0LW9wZW5hcGkgYW5kIGlvLnN3YWdnZXIudjMucGFyc2VyfCBCW0pTT04gZmlsZV1cbkIgLS0-IHxQYXJzZWQgd2l0aCBjaXJjZSB8IEMoU3dhZ2dlckFTVClcbkMgLS0-IHxBU1RUcmFuc2xhdGlvbkZ1bmN0aW9uc3wgRChUeXBlUmVwKVxuRCAtLT58U2NhbGFHZW5lcmF0b3J8IEZbU2NhbGEgRmlsZXNdXG5EIC0tPnxQb3NzaWJseU1vcmVHZW5lcmF0b3JzfCBHW090aGVyIEZpbGVzXVxuIiwibWVybWFpZCI6eyJ0aGVtZSI6ImRlZmF1bHQiLCJ0aGVtZVZhcmlhYmxlcyI6eyJiYWNrZ3JvdW5kIjoid2hpdGUiLCJwcmltYXJ5Q29sb3IiOiIjRUNFQ0ZGIiwic2Vjb25kYXJ5Q29sb3IiOiIjZmZmZmRlIiwidGVydGlhcnlDb2xvciI6ImhzbCg4MCwgMTAwJSwgOTYuMjc0NTA5ODAzOSUpIiwicHJpbWFyeUJvcmRlckNvbG9yIjoiaHNsKDI0MCwgNjAlLCA4Ni4yNzQ1MDk4MDM5JSkiLCJzZWNvbmRhcnlCb3JkZXJDb2xvciI6ImhzbCg2MCwgNjAlLCA4My41Mjk0MTE3NjQ3JSkiLCJ0ZXJ0aWFyeUJvcmRlckNvbG9yIjoiaHNsKDgwLCA2MCUsIDg2LjI3NDUwOTgwMzklKSIsInByaW1hcnlUZXh0Q29sb3IiOiIjMTMxMzAwIiwic2Vjb25kYXJ5VGV4dENvbG9yIjoiIzAwMDAyMSIsInRlcnRpYXJ5VGV4dENvbG9yIjoicmdiKDkuNTAwMDAwMDAwMSwgOS41MDAwMDAwMDAxLCA5LjUwMDAwMDAwMDEpIiwibGluZUNvbG9yIjoiIzMzMzMzMyIsInRleHRDb2xvciI6IiMzMzMiLCJtYWluQmtnIjoiI0VDRUNGRiIsInNlY29uZEJrZyI6IiNmZmZmZGUiLCJib3JkZXIxIjoiIzkzNzBEQiIsImJvcmRlcjIiOiIjYWFhYTMzIiwiYXJyb3doZWFkQ29sb3IiOiIjMzMzMzMzIiwiZm9udEZhbWlseSI6IlwidHJlYnVjaGV0IG1zXCIsIHZlcmRhbmEsIGFyaWFsIiwiZm9udFNpemUiOiIxNnB4IiwibGFiZWxCYWNrZ3JvdW5kIjoiI2U4ZThlOCIsIm5vZGVCa2ciOiIjRUNFQ0ZGIiwibm9kZUJvcmRlciI6IiM5MzcwREIiLCJjbHVzdGVyQmtnIjoiI2ZmZmZkZSIsImNsdXN0ZXJCb3JkZXIiOiIjYWFhYTMzIiwiZGVmYXVsdExpbmtDb2xvciI6IiMzMzMzMzMiLCJ0aXRsZUNvbG9yIjoiIzMzMyIsImVkZ2VMYWJlbEJhY2tncm91bmQiOiIjZThlOGU4IiwiYWN0b3JCb3JkZXIiOiJoc2woMjU5LjYyNjE2ODIyNDMsIDU5Ljc3NjUzNjMxMjglLCA4Ny45MDE5NjA3ODQzJSkiLCJhY3RvckJrZyI6IiNFQ0VDRkYiLCJhY3RvclRleHRDb2xvciI6ImJsYWNrIiwiYWN0b3JMaW5lQ29sb3IiOiJncmV5Iiwic2lnbmFsQ29sb3IiOiIjMzMzIiwic2lnbmFsVGV4dENvbG9yIjoiIzMzMyIsImxhYmVsQm94QmtnQ29sb3IiOiIjRUNFQ0ZGIiwibGFiZWxCb3hCb3JkZXJDb2xvciI6ImhzbCgyNTkuNjI2MTY4MjI0MywgNTkuNzc2NTM2MzEyOCUsIDg3LjkwMTk2MDc4NDMlKSIsImxhYmVsVGV4dENvbG9yIjoiYmxhY2siLCJsb29wVGV4dENvbG9yIjoiYmxhY2siLCJub3RlQm9yZGVyQ29sb3IiOiIjYWFhYTMzIiwibm90ZUJrZ0NvbG9yIjoiI2ZmZjVhZCIsIm5vdGVUZXh0Q29sb3IiOiJibGFjayIsImFjdGl2YXRpb25Cb3JkZXJDb2xvciI6IiM2NjYiLCJhY3RpdmF0aW9uQmtnQ29sb3IiOiIjZjRmNGY0Iiwic2VxdWVuY2VOdW1iZXJDb2xvciI6IndoaXRlIiwic2VjdGlvbkJrZ0NvbG9yIjoicmdiYSgxMDIsIDEwMiwgMjU1LCAwLjQ5KSIsImFsdFNlY3Rpb25Ca2dDb2xvciI6IndoaXRlIiwic2VjdGlvbkJrZ0NvbG9yMiI6IiNmZmY0MDAiLCJ0YXNrQm9yZGVyQ29sb3IiOiIjNTM0ZmJjIiwidGFza0JrZ0NvbG9yIjoiIzhhOTBkZCIsInRhc2tUZXh0TGlnaHRDb2xvciI6IndoaXRlIiwidGFza1RleHRDb2xvciI6IndoaXRlIiwidGFza1RleHREYXJrQ29sb3IiOiJibGFjayIsInRhc2tUZXh0T3V0c2lkZUNvbG9yIjoiYmxhY2siLCJ0YXNrVGV4dENsaWNrYWJsZUNvbG9yIjoiIzAwMzE2MyIsImFjdGl2ZVRhc2tCb3JkZXJDb2xvciI6IiM1MzRmYmMiLCJhY3RpdmVUYXNrQmtnQ29sb3IiOiIjYmZjN2ZmIiwiZ3JpZENvbG9yIjoibGlnaHRncmV5IiwiZG9uZVRhc2tCa2dDb2xvciI6ImxpZ2h0Z3JleSIsImRvbmVUYXNrQm9yZGVyQ29sb3IiOiJncmV5IiwiY3JpdEJvcmRlckNvbG9yIjoiI2ZmODg4OCIsImNyaXRCa2dDb2xvciI6InJlZCIsInRvZGF5TGluZUNvbG9yIjoicmVkIiwibGFiZWxDb2xvciI6ImJsYWNrIiwiZXJyb3JCa2dDb2xvciI6IiM1NTIyMjIiLCJlcnJvclRleHRDb2xvciI6IiM1NTIyMjIiLCJjbGFzc1RleHQiOiIjMTMxMzAwIiwiZmlsbFR5cGUwIjoiI0VDRUNGRiIsImZpbGxUeXBlMSI6IiNmZmZmZGUiLCJmaWxsVHlwZTIiOiJoc2woMzA0LCAxMDAlLCA5Ni4yNzQ1MDk4MDM5JSkiLCJmaWxsVHlwZTMiOiJoc2woMTI0LCAxMDAlLCA5My41Mjk0MTE3NjQ3JSkiLCJmaWxsVHlwZTQiOiJoc2woMTc2LCAxMDAlLCA5Ni4yNzQ1MDk4MDM5JSkiLCJmaWxsVHlwZTUiOiJoc2woLTQsIDEwMCUsIDkzLjUyOTQxMTc2NDclKSIsImZpbGxUeXBlNiI6ImhzbCg4LCAxMDAlLCA5Ni4yNzQ1MDk4MDM5JSkiLCJmaWxsVHlwZTciOiJoc2woMTg4LCAxMDAlLCA5My41Mjk0MTE3NjQ3JSkifX0sInVwZGF0ZUVkaXRvciI6ZmFsc2V9](https://mermaid.ink/svg/eyJjb2RlIjoiZ3JhcGggVERcbkFbWWFtbCBmaWxlc10gLS0-fENvbWJpbmVkIHdpdGggc2J0LW9wZW5hcGkgYW5kIGlvLnN3YWdnZXIudjMucGFyc2VyfCBCW0pTT04gZmlsZV1cbkIgLS0-IHxQYXJzZWQgd2l0aCBjaXJjZSB8IEMoU3dhZ2dlckFTVClcbkMgLS0-IHxBU1RUcmFuc2xhdGlvbkZ1bmN0aW9uc3wgRChUeXBlUmVwKVxuRCAtLT58U2NhbGFHZW5lcmF0b3J8IEZbU2NhbGEgRmlsZXNdXG5EIC0tPnxQb3NzaWJseU1vcmVHZW5lcmF0b3JzfCBHW090aGVyIEZpbGVzXVxuIiwibWVybWFpZCI6eyJ0aGVtZSI6ImRlZmF1bHQiLCJ0aGVtZVZhcmlhYmxlcyI6eyJiYWNrZ3JvdW5kIjoid2hpdGUiLCJwcmltYXJ5Q29sb3IiOiIjRUNFQ0ZGIiwic2Vjb25kYXJ5Q29sb3IiOiIjZmZmZmRlIiwidGVydGlhcnlDb2xvciI6ImhzbCg4MCwgMTAwJSwgOTYuMjc0NTA5ODAzOSUpIiwicHJpbWFyeUJvcmRlckNvbG9yIjoiaHNsKDI0MCwgNjAlLCA4Ni4yNzQ1MDk4MDM5JSkiLCJzZWNvbmRhcnlCb3JkZXJDb2xvciI6ImhzbCg2MCwgNjAlLCA4My41Mjk0MTE3NjQ3JSkiLCJ0ZXJ0aWFyeUJvcmRlckNvbG9yIjoiaHNsKDgwLCA2MCUsIDg2LjI3NDUwOTgwMzklKSIsInByaW1hcnlUZXh0Q29sb3IiOiIjMTMxMzAwIiwic2Vjb25kYXJ5VGV4dENvbG9yIjoiIzAwMDAyMSIsInRlcnRpYXJ5VGV4dENvbG9yIjoicmdiKDkuNTAwMDAwMDAwMSwgOS41MDAwMDAwMDAxLCA5LjUwMDAwMDAwMDEpIiwibGluZUNvbG9yIjoiIzMzMzMzMyIsInRleHRDb2xvciI6IiMzMzMiLCJtYWluQmtnIjoiI0VDRUNGRiIsInNlY29uZEJrZyI6IiNmZmZmZGUiLCJib3JkZXIxIjoiIzkzNzBEQiIsImJvcmRlcjIiOiIjYWFhYTMzIiwiYXJyb3doZWFkQ29sb3IiOiIjMzMzMzMzIiwiZm9udEZhbWlseSI6IlwidHJlYnVjaGV0IG1zXCIsIHZlcmRhbmEsIGFyaWFsIiwiZm9udFNpemUiOiIxNnB4IiwibGFiZWxCYWNrZ3JvdW5kIjoiI2U4ZThlOCIsIm5vZGVCa2ciOiIjRUNFQ0ZGIiwibm9kZUJvcmRlciI6IiM5MzcwREIiLCJjbHVzdGVyQmtnIjoiI2ZmZmZkZSIsImNsdXN0ZXJCb3JkZXIiOiIjYWFhYTMzIiwiZGVmYXVsdExpbmtDb2xvciI6IiMzMzMzMzMiLCJ0aXRsZUNvbG9yIjoiIzMzMyIsImVkZ2VMYWJlbEJhY2tncm91bmQiOiIjZThlOGU4IiwiYWN0b3JCb3JkZXIiOiJoc2woMjU5LjYyNjE2ODIyNDMsIDU5Ljc3NjUzNjMxMjglLCA4Ny45MDE5NjA3ODQzJSkiLCJhY3RvckJrZyI6IiNFQ0VDRkYiLCJhY3RvclRleHRDb2xvciI6ImJsYWNrIiwiYWN0b3JMaW5lQ29sb3IiOiJncmV5Iiwic2lnbmFsQ29sb3IiOiIjMzMzIiwic2lnbmFsVGV4dENvbG9yIjoiIzMzMyIsImxhYmVsQm94QmtnQ29sb3IiOiIjRUNFQ0ZGIiwibGFiZWxCb3hCb3JkZXJDb2xvciI6ImhzbCgyNTkuNjI2MTY4MjI0MywgNTkuNzc2NTM2MzEyOCUsIDg3LjkwMTk2MDc4NDMlKSIsImxhYmVsVGV4dENvbG9yIjoiYmxhY2siLCJsb29wVGV4dENvbG9yIjoiYmxhY2siLCJub3RlQm9yZGVyQ29sb3IiOiIjYWFhYTMzIiwibm90ZUJrZ0NvbG9yIjoiI2ZmZjVhZCIsIm5vdGVUZXh0Q29sb3IiOiJibGFjayIsImFjdGl2YXRpb25Cb3JkZXJDb2xvciI6IiM2NjYiLCJhY3RpdmF0aW9uQmtnQ29sb3IiOiIjZjRmNGY0Iiwic2VxdWVuY2VOdW1iZXJDb2xvciI6IndoaXRlIiwic2VjdGlvbkJrZ0NvbG9yIjoicmdiYSgxMDIsIDEwMiwgMjU1LCAwLjQ5KSIsImFsdFNlY3Rpb25Ca2dDb2xvciI6IndoaXRlIiwic2VjdGlvbkJrZ0NvbG9yMiI6IiNmZmY0MDAiLCJ0YXNrQm9yZGVyQ29sb3IiOiIjNTM0ZmJjIiwidGFza0JrZ0NvbG9yIjoiIzhhOTBkZCIsInRhc2tUZXh0TGlnaHRDb2xvciI6IndoaXRlIiwidGFza1RleHRDb2xvciI6IndoaXRlIiwidGFza1RleHREYXJrQ29sb3IiOiJibGFjayIsInRhc2tUZXh0T3V0c2lkZUNvbG9yIjoiYmxhY2siLCJ0YXNrVGV4dENsaWNrYWJsZUNvbG9yIjoiIzAwMzE2MyIsImFjdGl2ZVRhc2tCb3JkZXJDb2xvciI6IiM1MzRmYmMiLCJhY3RpdmVUYXNrQmtnQ29sb3IiOiIjYmZjN2ZmIiwiZ3JpZENvbG9yIjoibGlnaHRncmV5IiwiZG9uZVRhc2tCa2dDb2xvciI6ImxpZ2h0Z3JleSIsImRvbmVUYXNrQm9yZGVyQ29sb3IiOiJncmV5IiwiY3JpdEJvcmRlckNvbG9yIjoiI2ZmODg4OCIsImNyaXRCa2dDb2xvciI6InJlZCIsInRvZGF5TGluZUNvbG9yIjoicmVkIiwibGFiZWxDb2xvciI6ImJsYWNrIiwiZXJyb3JCa2dDb2xvciI6IiM1NTIyMjIiLCJlcnJvclRleHRDb2xvciI6IiM1NTIyMjIiLCJjbGFzc1RleHQiOiIjMTMxMzAwIiwiZmlsbFR5cGUwIjoiI0VDRUNGRiIsImZpbGxUeXBlMSI6IiNmZmZmZGUiLCJmaWxsVHlwZTIiOiJoc2woMzA0LCAxMDAlLCA5Ni4yNzQ1MDk4MDM5JSkiLCJmaWxsVHlwZTMiOiJoc2woMTI0LCAxMDAlLCA5My41Mjk0MTE3NjQ3JSkiLCJmaWxsVHlwZTQiOiJoc2woMTc2LCAxMDAlLCA5Ni4yNzQ1MDk4MDM5JSkiLCJmaWxsVHlwZTUiOiJoc2woLTQsIDEwMCUsIDkzLjUyOTQxMTc2NDclKSIsImZpbGxUeXBlNiI6ImhzbCg4LCAxMDAlLCA5Ni4yNzQ1MDk4MDM5JSkiLCJmaWxsVHlwZTciOiJoc2woMTg4LCAxMDAlLCA5My41Mjk0MTE3NjQ3JSkifX0sInVwZGF0ZUVkaXRvciI6ZmFsc2V9)

### sbt-openapi
The SBT module `sbt-openapi` is the interface for the generator toolchain and contains:
1. an interface to easily add the openAPI generator to a project
1. Put external openAPI resources/definitions/yaml-files next to internal definitions to make them referable. This enables sharing artifact across multiple openAPI projects (like shared error messages/ data structures).
1. logic to load, resolve and aggregate openAPI definitions using the io.swagger.parser and generated a single combined openapi.json definition as output
1. trigger code generation. (`openapi-scala`) 

### openapi-scala
`openapi-scala`s main responsibility is to parse a single openAPI.json file into memory (`ast`), transform it into a
generator friendly intermediate representation (`repr`) and generate scala code (`generator`).

### openapi-lib & openapi-htt4s-lib
Libraries used by the generated code.

#### Routes

We support different types of Route generation, depending on the backend you need it for.  //TODO which backends do we have and how do we configure it?

- The generic Routes are each translated to a Scala `trait` declaring interfaces for that particular HTTP route.
- The http4s Routes translate into two files, one file with a trait for the implementation you'll need to provide and 
one file with an object apply function of which accepts mentioned implementation trait. 

The names of the functions implementing the route are either
 a) the concatenation of http method and path or
 b) the optionally more descriptive content of the `operationId` field (as specified in OAS 3.1) 
 
## Components

Components in OpenAPI are the types that can be referred to as inputs/outputs for routes. A common use of components is 
to define product types. We take these components and translate them to Scala case classes. Consider the following example:

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

## Refinements

OAS 3.1 allows one to set constraints on the primitive input types. We use the [Refined](https://github.com/fthomas/refined) library for Scala to reproduce these in the generated code. This means that you do not need to manually setup validations for any refinements and constraints in the inputs for your API. This, however, also means that you will have to return values with refinements if there are any in the return types.

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

OpenAPI 3.1 supports marking properties in objects as read-only. This checks that the property is available if the type is used as the output of a route, but not for inputs.

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

OpenAPI 3.1 uses `oneOf` types to represent unions or enumerations. In Scala, there is no way to have proper Algebraic Data Types (ADTs). While products can be represented using case classes, the only way to represent unions is to create classes that extend a given trait.

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

This generator is highly opinionated and skips over certain features defined OpenAPI 3.1 specification for the sake of API consistency and compatibility with modern typed languages.

### Limitations

- We do not support any anonymous types for representing HTTP requests and responses.
- We do not support nested type definitions. Therefore, any objects that are used must be defined in the components and must not contain
  any other nested objects. Nested arrays, however, are supported.
- We do not support more than one type for Content-Type header for incoming requests. Multiple types corresponding to their encodings are, however, available with an Accept-Encoding header on a request.
- We do not support `anyOf` types from OpenAPI 3.1. Suggestions as to how the behaviour for the same should be represented in generated Scala code are welcome.
- We do not support inlined schema definition for input and output object types in route definitions in OpenAPI input files. Therefore, these must be defined as components and referred.
- We do not support type aliasing or rich typing. For instance, if you have a primitive type with a particular set of refinements, you would have to point out those refinements everywhere with those refinements instead of referring them from a definition.
