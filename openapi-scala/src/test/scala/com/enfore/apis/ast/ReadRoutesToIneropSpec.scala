package com.enfore.apis.ast

import com.enfore.apis.ast.SwaggerAST.{
  Component,
  CoreASTRepr,
  MediaTypeObject,
  ParamObject,
  PathObject,
  Property,
  SchemaObject,
  SchemaRefContainer,
  SchemaStore
}
import com.enfore.apis.ast.SwaggerAST.PropertyType.{`object` => _, _}
import com.enfore.apis.ast.SwaggerAST.ComponentType.{`object`, string => componentString}
import com.enfore.apis.ast.SwaggerAST.ParameterLocation.path
import com.enfore.apis.repr.TypeRepr.ReqWithContentType.PUT
import com.enfore.apis.repr.TypeRepr.{GetRequest, PathItemAggregation, PathParameter, PutOrPostRequest, Ref}
import org.scalatest.{FlatSpec, Matchers}

class ReadRoutesToIneropSpec extends FlatSpec with Matchers {
  "readRoutesToInerop" should "be able to read multiple methods per route" in {
    val expected = Map(
      "_contacts_individual_{contact-id}" ->
        PathItemAggregation(
          "/contacts/individual/{contact-id}",
          List(
            GetRequest(
              "/contacts/individual/{contact-id}",
              List(PathParameter("contact-id")),
              Map(),
              Some(
                Map("application/vnd.enfore.contacts+json" -> Ref(
                  "#/components/schemas/IndividualContact",
                  "IndividualContact")))
            ),
            PutOrPostRequest(
              "/contacts/individual/{contact-id}",
              PUT,
              List(PathParameter("contact-id")),
              Map(),
              Ref("#/components/schemas/IndividualContact", "IndividualContact"),
              Some(
                Map("application/vnd.enfore.contacts+json" -> Ref(
                  "#/components/schemas/IndividualContact",
                  "IndividualContact")))
            )
          )
        ))

    val actual = ASTTranslationFunctions.readRoutesToInerop(ast)

    normalise(actual) should equal(normalise(expected))
  }

  private def normalise(in: Map[String, PathItemAggregation]): Map[String, PathItemAggregation] =
    in.mapValues { p =>
      p.copy(
        items = p.items.sortBy(_.toString) // Sort request types
      )
    }

  private lazy val ast = CoreASTRepr(
    SchemaStore(
      Map(
        "Contact" -> Component(
          Some("Base type for all contacts.\n"),
          `object`,
          Some(
            Map(
              "id" -> Property(
                Some("The technical identifier of the contact. Assigned by the enfore platform on creation of the contact and not changeable afterwards."),
                Some(string),
                None,
                None,
                Some(true),
                None,
                None
              ),
              "type" -> Property(None, Some(string), None, None, None, None, None),
              "name" -> Property(
                Some(
                  "The name of the contact.\nFor individual contacts, this is the full name of the person, e.g., \"Barnabas Ludwig Johnson II Sr.\".\n" +
                    "For organization contacts, this is the \"common name\" of the organization. This may not necessarily be the registered business name, " +
                    "as that may either not exist or be too cumbersome to use.\n"),
                Some(string),
                None,
                None,
                None,
                None,
                None
              )
            )
          ),
          None,
          Some(List("id", "type", "name"))
        ),
        "IndividualContact" -> Component(
          Some("Represents a human person.\n"),
          `object`,
          Some(Map(
            "name"        -> Property(None, Some(string), None, None, None, None, None),
            "dateOfBirth" -> Property(None, Some(string), None, None, None, None, None),
            "id"          -> Property(None, Some(string), None, None, None, None, None),
            "anniversary" -> Property(None, Some(string), None, None, None, None, None),
            "gender"      -> Property(None, None, None, Some("#/components/schemas/Gender"), None, None, None)
          )),
          None,
          None
        ),
        "Gender" -> Component(
          Some("The gender of a human person"),
          componentString,
          None,
          Some(List("FEMALE", "MALE")),
          None)
      )),
    Some(
      Map(
        "/contacts/individual/{contact-id}" -> Map(
          "get" -> PathObject(
            Some("Load an IndividualContact by its identifier"),
            None,
            Map(
              200 -> MediaTypeObject(Some(Map("application/vnd.enfore.contacts+json" -> SchemaRefContainer(
                Some(SchemaObject(Some("#/components/schemas/IndividualContact"))))))),
              403 -> MediaTypeObject(None),
              404 -> MediaTypeObject(None)
            ),
            Some(List(ParamObject("contact-id", path, Some(SchemaObject(None)), Some("ID of the contact to load"))))
          ),
          "put" -> PathObject(
            Some("Full update of an IndividualContact"),
            Some(MediaTypeObject(Some(Map("application/vnd.enfore.contacts+json" -> SchemaRefContainer(
              Some(SchemaObject(Some("#/components/schemas/IndividualContact")))))))),
            Map(
              200 -> MediaTypeObject(Some(Map("application/vnd.enfore.contacts+json" -> SchemaRefContainer(
                Some(SchemaObject(Some("#/components/schemas/IndividualContact"))))))),
              400 -> MediaTypeObject(None),
              403 -> MediaTypeObject(None),
              404 -> MediaTypeObject(None)
            ),
            Some(List(ParamObject("contact-id", path, Some(SchemaObject(None)), Some("ID of the contact to update"))))
          )
        ))
    )
  )
}
