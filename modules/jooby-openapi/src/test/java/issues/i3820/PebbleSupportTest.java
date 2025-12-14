/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.internal.openapi.asciidoc.PebbleTemplateSupport;
import io.jooby.openapi.CurrentDir;
import io.jooby.openapi.OpenAPITest;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.SpecVersion;
import issues.i3729.api.AppLibrary;
import issues.i3820.app.AppLib;

public class PebbleSupportTest {

  @OpenAPITest(value = AppLib.class)
  public void routes(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);

    templates
        .evaluateThat("{{ routes(\"/library/books/?.*\") | table(grid=\"rows\") }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,3a", grid="rows"]
            |===
            |Method|Path|Summary

            |`+GET+`
            |`+/library/books/{isbn}+`
            |Get Specific Book Details

            |`+GET+`
            |`+/library/books+`
            |Browse Books (Paginated)

            |`+POST+`
            |`+/library/books+`
            |Add New Book

            |===\
            """);

    // default error map
    templates
        .evaluateThat("{{ routes }}")
        .isEqualTo(
            "[GET /library/books/{isbn}, GET /library/search, GET /library/books, POST"
                + " /library/books, POST /library/authors]");
    templates
        .evaluateThat("{{ routes(\"/library/books/?.*\") }}")
        .isEqualTo("[GET /library/books/{isbn}, GET /library/books, POST /library/books]");
    templates.evaluate("{{ routes | json(false) }}", output -> Json31.mapper().readTree(output));

    templates
        .evaluateThat("{{ routes(\"/library/books/?.*\") | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,3a", options="header"]
            |===
            |Method|Path|Summary

            |`+GET+`
            |`+/library/books/{isbn}+`
            |Get Specific Book Details

            |`+GET+`
            |`+/library/books+`
            |Browse Books (Paginated)

            |`+POST+`
            |`+/library/books+`
            |Add New Book

            |===\
            """);

    templates
        .evaluateThat("{{ routes(\"/library/books/?.*\") | list }}")
        .isEqualToIgnoringNewLines(
            """
            * `+GET /library/books/{isbn}+`: Get Specific Book Details
            * `+GET /library/books+`: Browse Books (Paginated)
            * `+POST /library/books+`: Add New Book\
            """);
  }

  @OpenAPITest(value = AppLib.class)
  public void statusCode(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);
    // default error map
    templates.evaluateThat("{{ statusCode(200) }}").isEqualTo("[{code=200, reason=Success}]");

    templates
        .evaluateThat("{{ statusCode(200) | json }}")
        .isEqualToIgnoringNewLines(
            """
            [source, json]
            ----
            {
              "code" : 200,
              "reason" : "Success"
            }
            ----\
            """);

    templates
        .evaluateThat("{{ statusCode(200) | list }}")
        .isEqualToIgnoringNewLines(
            """
            * `+200+`: Success\
            """);

    templates
        .evaluateThat("{{ statusCode(200) | table }}")
        .isEqualToIgnoringNewLines(
            """
            |===
            |code|reason

            |200
            |Success

            |===\
            """);

    templates
        .evaluateThat("{{ statusCode([200, 201]) | table }}")
        .isEqualToIgnoringNewLines(
            """
            |===
            |code|reason

            |200
            |Success

            |201
            |Created

            |===\
            """);

    templates
        .evaluateThat("{{ statusCode([200, 201]) | list }}")
        .isEqualToIgnoringNewLines(
            """
            * `+200+`: Success
            * `+201+`: Created\
            """);

    templates
        .evaluateThat("{{ statusCode({200: \"OK\", 500: \"Internal Server Error\"}) | list }}")
        .isEqualToIgnoringNewLines(
            """
            * `+200+`: OK
            * `+500+`: Internal Server Error\
            """);
  }

  @OpenAPITest(value = AppLibrary.class)
  public void bodyBug(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);
    templates
        .evaluateThat("{{ GET(\"/api/library/{isbn}\") | response | body | json(false) }}")
        .isEqualToIgnoringNewLines(
            """
            {
              "isbn" : "string",
              "title" : "string",
              "publicationDate" : "date",
              "text" : "string",
              "type" : "string",
              "authors" : [ {
                "ssn" : "string",
                "name" : "string",
                "address" : {
                  "street" : "string",
                  "city" : "string",
                  "state" : "string",
                  "country" : "string"
                },
                "books" : [ { } ]
              } ],
              "image" : "binary"
            }\
            """);
  }

  @OpenAPITest(value = AppLib.class, version = SpecVersion.V31)
  public void shouldSupportJsonSchemaInV31(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);
    templates
        .evaluateThat("{{ POST(\"/library/books\") | request | body | json(false) }}")
        .isEqualToIgnoringNewLines(
            """
            {
              "isbn" : "string",
              "title" : "string",
              "publicationDate" : "date",
              "text" : "string",
              "type" : "string",
              "publisher" : {
                "id" : "int64",
                "name" : "string"
              },
              "authors" : [ {
                "ssn" : "string",
                "name" : "string",
                "address" : {
                  "street" : "string",
                  "city" : "string",
                  "zip" : "string"
                }
              } ]
            }\
            """);
  }

  @OpenAPITest(value = AppLib.class)
  public void errorMap(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);
    templates
        .evaluateThat(
            """
            {{ error(400) | json }}
            """)
        .isEqualToIgnoringNewLines(
            """
            [source, json]
            ----
            {
              "message" : "...",
              "reason" : "Bad Request",
              "statusCode" : 400
            }
            ----\
            """);
    // default error map
    templates
        .evaluateThat(
            """
            {{ error(code=400) | json }}
            """)
        .isEqualToIgnoringNewLines(
            """
            [source, json]
            ----
            {
              "message" : "...",
              "reason" : "Bad Request",
              "statusCode" : 400
            }
            ----\
            """);

    templates
        .evaluateThat(
            """
            {{ error(code=400) | list }}
            """)
        .isEqualToIgnoringNewLines(
            """
            * message: ...
            * reason: Bad Request
            * statusCode: 400\
            """);

    templates
        .evaluateThat(
            """
            {{ error(code=400) | table }}
            """)
        .isEqualToIgnoringNewLines(
            """
            |===
            |message|reason|statusCode

            |...
            |Bad Request
            |400

            |===\
            """);

    templates
        .evaluateThat(
            """
            {%- set error = {"code": 500, "message": "{{code.reason}}", "time": now } -%}
            {{ error(code=402) | json }}
            """)
        .isEqualToIgnoringNewLines(
            String.format(
                """
                [source, json]
                ----
                {
                  "code" : 402,
                  "message" : "Payment Required",
                  "time" : "%s"
                }
                ----\
                """,
                templates.getContext().getNow()));
  }

  @OpenAPITest(value = AppLib.class)
  public void openApi(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);
    templates.evaluate(
        "{{openapi | json(wrap=false) }}",
        output -> {
          Json31.mapper().readTree(output);
        });

    templates.evaluate(
        "{{GET(\"/library/search\") | json(false)}}",
        output -> {
          Json31.mapper().readTree(output);
        });

    templates.evaluate(
        "{{openapi | yaml}}",
        output -> {
          Yaml31.mapper().readTree(output);
        });

    templates.evaluate(
        "{{GET(\"/library/search\") | yaml}}",
        output -> {
          Yaml31.mapper().readTree(output);
        });
  }

  @OpenAPITest(value = AppLib.class)
  public void tags(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);
    templates
        .evaluateThat("{{tag(\"Library\").description }}")
        .isEqualToIgnoringNewLines(
            "Outlines the available actions in the Library System API. The system is designed to"
                + " allow users to search for books, view details, and manage the library"
                + " inventory.");

    templates
        .evaluateThat(
            """
            {% for tag in tags %}
            == {{ tag.name }}
            {{ tag.description }}

            // 2. Loop through all routes associated with this tag
            {% for route in tag.routes %}
            === {{ route.summary }}
            {{ route.description }}

            *URL:* `{{ route.path }}` ({{ route.method }})
            {% if route.parameters is not empty %}
            *Parameters:*
            {{ route | parameters | table }}
            {% endif %}
            // Only show Request Body if it exists (e.g. for POST/PUT)
            {% if route.body %}
            *Data Payload:*
            {{ route | request | body | example | json }}
            {% endif %}
            // Example response for success
            .Response
            {{ route | response(200) | json }}
            {% endfor %}
            {% endfor %}
            """)
        .isEqualToIgnoringNewLines(
            """
            """);
  }

  @OpenAPITest(value = AppLib.class)
  public void server(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);
    templates.evaluateThat("{{ server(0).url }}").isEqualTo("https://library.jooby.io");

    templates
        .evaluateThat("{{ server(\"Production\").url }}")
        .isEqualTo("https://library.jooby.io");
  }

  @OpenAPITest(value = AppLib.class)
  public void schema(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);

    templates
        .evaluateThat("{{schema(\"Book\") | truncate | json}}")
        .isEqualToIgnoringNewLines(
            """
            [source, json]
            ----
            {
              "isbn" : "string",
              "title" : "string",
              "publicationDate" : "date",
              "text" : "string",
              "type" : "string",
              "publisher" : { },
              "authors" : [ { } ]
            }
            ----\
            """);

    templates
        .evaluateThat("{{schema(\"Book\") | truncate | yaml(false) }}")
        .isEqualToIgnoringNewLines(
            """
            isbn: string
            title: string
            publicationDate: date
            text: string
            type: string
            publisher: {}
            authors:
            - {}\
            """);

    // example on same schema must generate same output
    var output = templates.evaluate("{{schema(\"Book\") | example | json}}");
    assertEquals(output, templates.evaluate("{{schema(\"Book\") | example | json}}"));

    var yamlOutput = templates.evaluate("{{model(\"Book\") | example | yaml}}");
    assertEquals(yamlOutput, templates.evaluate("{{model(\"Book\") | example | yaml}}"));

    templates
        .evaluateThat("{{schema(\"Book\") | json(false) }}")
        .isEqualToIgnoringNewLines(
            """
            {
              "isbn" : "string",
              "title" : "string",
              "publicationDate" : "date",
              "text" : "string",
              "type" : "string",
              "publisher" : {
                "id" : "int64",
                "name" : "string"
              },
              "authors" : [ {
                "ssn" : "string",
                "name" : "string",
                "address" : {
                  "street" : "string",
                  "city" : "string",
                  "zip" : "string"
                }
              } ]
            }
            """);

    templates
        .evaluateThat("{{schema(\"Address\") | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,3", options="header"]
            |===
            |Name|Type|Description
            |`+street+`
            |`+string+`
            |The specific street address. Includes the house number, street name, and apartment number if applicable. Example: "123 Maple Avenue, Apt 4B".

            |`+city+`
            |`+string+`
            |The town, city, or municipality. Used for grouping authors by location or calculating shipping regions.

            |`+zip+`
            |`+string+`
            |The postal or zip code. Stored as text (String) rather than a number to support codes that start with zero (e.g., "02138") or contain letters (e.g., "K1A 0B1").

            |===\
            """);

    templates
        .evaluateThat("{{schema(\"Address\") | list }}")
        .isEqualToIgnoringNewLines(
            """
            street::
            * type: `+string+`
            * description: The specific street address. Includes the house number, street name, and apartment number if applicable. Example: "123 Maple Avenue, Apt 4B".
            city::
            * type: `+string+`
            * description: The town, city, or municipality. Used for grouping authors by location or calculating shipping regions.
            zip::
            * type: `+string+`
            * description: The postal or zip code. Stored as text (String) rather than a number to support codes that start with zero (e.g., "02138") or contain letters (e.g., "K1A 0B1").\
            """);

    templates
        .evaluateThat("{{schema(\"Book.type\") | list }}")
        .isEqualToIgnoringNewLines(
            """
            *NOVEL*::
            * A fictional narrative story. Examples: "Pride and Prejudice", "Harry Potter", "Dune". These are creative works meant for entertainment or artistic expression.
            *BIOGRAPHY*::
            * A written account of a real person's life. Examples: "Steve Jobs" by Walter Isaacson, "The Diary of a Young Girl". These are non-fiction historical records of an individual.
            *TEXTBOOK*::
            * An educational book used for study. Examples: "Calculus: Early Transcendentals", "Introduction to Java Programming". These are designed for students and are often used as reference material in academic courses.
            *MAGAZINE*::
            * A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.
            *JOURNAL*::
            * A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.\
            """);
    templates
        .evaluateThat("{{schema(\"Book.type\") | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,3", options="header"]
            |===
            |Name|Description
            | *NOVEL*
            | A fictional narrative story. Examples: "Pride and Prejudice", "Harry Potter", "Dune". These are creative works meant for entertainment or artistic expression.
            | *BIOGRAPHY*
            | A written account of a real person's life. Examples: "Steve Jobs" by Walter Isaacson, "The Diary of a Young Girl". These are non-fiction historical records of an individual.
            | *TEXTBOOK*
            | An educational book used for study. Examples: "Calculus: Early Transcendentals", "Introduction to Java Programming". These are designed for students and are often used as reference material in academic courses.
            | *MAGAZINE*
            | A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.
            | *JOURNAL*
            | A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.
            |===\
            """);
  }

  @OpenAPITest(value = AppLib.class)
  public void curl(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);

    templates
        .evaluateThat("{{POST(\"/library/authors\") | curl }}")
        .isEqualToIgnoringNewLines(
            """
            [source]
            ----
            curl --data-urlencode "ssn=string"\\
                 --data-urlencode "name=string"\\
                 --data-urlencode "address.street=string"\\
                 --data-urlencode "address.city=string"\\
                 --data-urlencode "address.zip=string"\\
                 -X POST '/library/authors'
            ----\
            """);

    templates
        .evaluateThat("{{POST(\"/library/books\") | request | curl }}")
        .isEqualToIgnoringNewLines(
            """
            [source]
            ----
            curl -H 'Accept: application/json'\\
                 -H 'Content-Type: application/json'\\
                 -d '{"isbn":"string","title":"string","publicationDate":"date","text":"string","type":"string","publisher":{"id":"int64","name":"string"},"authors":[{"ssn":"string","name":"string","address":{"street":"string","city":"string","zip":"string"}}]}'\\
                 -X POST '/library/books'
            ----\
            """);

    templates
        .evaluateThat("{{GET(\"/library/books\") | request | curl }}")
        .isEqualToIgnoringNewLines(
            """
            [source]
            ----
            curl -H 'Accept: application/json'\\
                 -X GET '/library/books?title=string&page=int32&size=int32'
            ----\
            """);

    templates
        .evaluateThat("{{GET(\"/library/books/{isbn}\") | request | curl }}")
        .isEqualToIgnoringNewLines(
            """
            [source]
            ----
            curl -X GET '/library/books/{isbn}'
            ----\
            """);

    templates
        .evaluateThat("{{GET(\"/library/books/{isbn}\") | request | curl(\"-i\") }}")
        .isEqualToIgnoringNewLines(
            """
            [source]
            ----
            curl -i\\
                 -X GET '/library/books/{isbn}'
            ----\
            """);

    templates
        .evaluateThat(
            "{{GET(\"/library/books/{isbn}\") | request | curl(\"-i\", \"-X\", \"POST\") }}")
        .isEqualToIgnoringNewLines(
            """
            [source]
            ----
            curl -i\\
                 -X POST '/library/books/{isbn}'
            ----\
            """);

    templates
        .evaluateThat(
            "{{GET(\"/library/books/{isbn}\") | request | curl(\"-i\", \"-X\", \"POST\") }}")
        .isEqualToIgnoringNewLines(
            """
            [source]
            ----
            curl -i\\
                 -X POST '/library/books/{isbn}'
            ----\
            """);

    templates
        .evaluateThat(
            "{{GET(\"/library/books\") | request | curl(\"-H\", \"'Accept: application/xml'\") }}")
        .isEqualToIgnoringNewLines(
            """
            [source]
            ----
            curl -H 'Accept: application/xml'\\
                 -X GET '/library/books?title=string&page=int32&size=int32'
            ----\
            """);
  }

  @OpenAPITest(value = AppLib.class)
  public void response(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);

    /* Error response code: */
    templates
        .evaluateThat("{{GET(\"/library/books/{isbn}\") | response(code=404) | json }}")
        .isEqualToIgnoringNewLines(
            """
            [source, json]
            ----
            {
              "message" : "Not Found: error if it doesn't exist.",
              "reason" : "Not Found",
              "statusCode" : 404
            }
            ----\
            """);

    templates
        .evaluateThat("{{GET(\"/library/books/{isbn}\") | response(code=404) | http }}")
        .isEqualToIgnoringNewLines(
            """
            [source,http,options="nowrap"]
            ----
            HTTP/1.1 404 Not Found
            ----\
            """);

    /* Override default response code: */
    templates
        .evaluateThat("{{POST(\"/library/books\") | response(code=201) | http }}")
        .isEqualToIgnoringNewLines(
            """
            [source,http,options="nowrap"]
            ----
            HTTP/1.1 201 Created
            Content-Type: application/json
            {"isbn":"string","title":"string","publicationDate":"date","text":"string","type":"string","publisher":{"id":"int64","name":"string"},"authors":[{"ssn":"string","name":"string","address":{"street":"string","city":"string","zip":"string"}}]}
            ----\
            """);

    /* Default response */
    templates
        .evaluateThat("{{POST(\"/library/books\") | response | http }}")
        .isEqualToIgnoringNewLines(
            """
            [source,http,options="nowrap"]
            ----
            HTTP/1.1 200 Success
            Content-Type: application/json
            {"isbn":"string","title":"string","publicationDate":"date","text":"string","type":"string","publisher":{"id":"int64","name":"string"},"authors":[{"ssn":"string","name":"string","address":{"street":"string","city":"string","zip":"string"}}]}
            ----\
            """);

    templates
        .evaluateThat("{{POST(\"/library/books\") | response | list }}")
        .isEqualToIgnoringNewLines(
            """
            isbn::
            * type: `+string+`
            * description: The unique "barcode" for this book (ISBN). We use this to identify exactly which book edition we are talking about.
            title::
            * type: `+string+`
            * description: The name printed on the cover.
            publicationDate::
            * type: `+date+`
            * description: When this book was released to the public.
            text::
            * type: `+string+`
            * description: The full story or content of the book. Since this can be very long, we store it in a special way (Large Object) to keep the database fast.
            type::
            * type: `+string+`
            * description: Defines the format and release schedule of the item.
            ** *NOVEL*: A fictional narrative story. Examples: "Pride and Prejudice", "Harry Potter", "Dune". These are creative works meant for entertainment or artistic expression.
            ** *BIOGRAPHY*: A written account of a real person's life. Examples: "Steve Jobs" by Walter Isaacson, "The Diary of a Young Girl". These are non-fiction historical records of an individual.
            ** *TEXTBOOK*: An educational book used for study. Examples: "Calculus: Early Transcendentals", "Introduction to Java Programming". These are designed for students and are often used as reference material in academic courses.
            ** *MAGAZINE*: A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.
            ** *JOURNAL*: A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.
            publisher::
            * type: `+object+`
            * description: A company that produces and sells books.
            authors::
            * type: `+array+`
            * description: The list of people who wrote this book.\
            """);

    templates
        .evaluateThat("{{POST(\"/library/books\") | response | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,3a", options="header"]
            |===
            |Name|Type|Description
            |`+isbn+`
            |`+string+`
            |The unique "barcode" for this book (ISBN). We use this to identify exactly which book edition we are talking about.

            |`+title+`
            |`+string+`
            |The name printed on the cover.

            |`+publicationDate+`
            |`+date+`
            |When this book was released to the public.

            |`+text+`
            |`+string+`
            |The full story or content of the book. Since this can be very long, we store it in a special way (Large Object) to keep the database fast.

            |`+type+`
            |`+string+`
            |Defines the format and release schedule of the item.

            * *NOVEL*: A fictional narrative story. Examples: "Pride and Prejudice", "Harry Potter", "Dune". These are creative works meant for entertainment or artistic expression.
            * *BIOGRAPHY*: A written account of a real person's life. Examples: "Steve Jobs" by Walter Isaacson, "The Diary of a Young Girl". These are non-fiction historical records of an individual.
            * *TEXTBOOK*: An educational book used for study. Examples: "Calculus: Early Transcendentals", "Introduction to Java Programming". These are designed for students and are often used as reference material in academic courses.
            * *MAGAZINE*: A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.
            * *JOURNAL*: A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.

            |`+publisher+`
            |`+object+`
            |A company that produces and sells books.

            |`+authors+`
            |`+array+`
            |The list of people who wrote this book.

            |===\
            """);
  }

  @OpenAPITest(value = AppLib.class)
  public void request(OpenAPIExt openapi) throws IOException {
    var templates = new PebbleTemplateSupport(CurrentDir.testClass(getClass(), "adoc"), openapi);

    templates
        .evaluateThat("{{GET(\"/library/books\") | request | path }}")
        .isEqualTo("/library/books?title=string&page=int32&size=int32");

    templates
        .evaluateThat("{{GET(\"/library/books\") | request | path(title=\"...\") }}")
        .isEqualTo("/library/books?title=...");

    templates
        .evaluateThat("{{GET(\"/library/books\") | request | path(title=\"...\", page=1) }}")
        .isEqualTo("/library/books?title=...&page=1");

    templates
        .evaluateThat("{{GET(\"/library/books\") | request | path(title=\"word space\", page=1) }}")
        .isEqualTo("/library/books?title=word%20space&page=1");

    templates
        .evaluateThat("{{GET(\"/library/books/{isbn}\") | request | path }}")
        .isEqualTo("/library/books/{isbn}");

    templates
        .evaluateThat("{{GET(\"/library/books/{isbn}\") | request | path(isbn=123) }}")
        .isEqualTo("/library/books/123");

    templates
        .evaluateThat("{{POST(\"/library/books\") | request | list }}")
        .isEqualToIgnoringNewLines(
            """
            Accept::
            * type: `+string+`
            * in: `+header+`
            Content-Type::
            * type: `+string+`
            * in: `+header+`
            isbn::
            * type: `+string+`
            * in: `+body+`
            * description: The unique "barcode" for this book (ISBN). We use this to identify exactly which book edition we are talking about.
            title::
            * type: `+string+`
            * in: `+body+`
            * description: The name printed on the cover.
            publicationDate::
            * type: `+date+`
            * in: `+body+`
            * description: When this book was released to the public.
            text::
            * type: `+string+`
            * in: `+body+`
            * description: The full story or content of the book. Since this can be very long, we store it in a special way (Large Object) to keep the database fast.
            type::
            * type: `+string+`
            * in: `+body+`
            * description: Defines the format and release schedule of the item.
            ** *NOVEL*: A fictional narrative story. Examples: "Pride and Prejudice", "Harry Potter", "Dune". These are creative works meant for entertainment or artistic expression.
            ** *BIOGRAPHY*: A written account of a real person's life. Examples: "Steve Jobs" by Walter Isaacson, "The Diary of a Young Girl". These are non-fiction historical records of an individual.
            ** *TEXTBOOK*: An educational book used for study. Examples: "Calculus: Early Transcendentals", "Introduction to Java Programming". These are designed for students and are often used as reference material in academic courses.
            ** *MAGAZINE*: A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.
            ** *JOURNAL*: A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.
            publisher::
            * type: `+object+`
            * in: `+body+`
            * description: A company that produces and sells books.
            authors::
            * type: `+array+`
            * in: `+body+`
            * description: The list of people who wrote this book.\
            """);

    templates
        .evaluateThat("{{POST(\"/library/books\") | request | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,1,3a", options="header"]
            |===
            |Name|Type|In|Description
            |`+Accept+`
            |`+string+`
            |`+header+`
            |

            |`+Content-Type+`
            |`+string+`
            |`+header+`
            |

            |`+isbn+`
            |`+string+`
            |`+body+`
            |The unique "barcode" for this book (ISBN). We use this to identify exactly which book edition we are talking about.

            |`+title+`
            |`+string+`
            |`+body+`
            |The name printed on the cover.

            |`+publicationDate+`
            |`+date+`
            |`+body+`
            |When this book was released to the public.

            |`+text+`
            |`+string+`
            |`+body+`
            |The full story or content of the book. Since this can be very long, we store it in a special way (Large Object) to keep the database fast.

            |`+type+`
            |`+string+`
            |`+body+`
            |Defines the format and release schedule of the item.

            * *NOVEL*: A fictional narrative story. Examples: "Pride and Prejudice", "Harry Potter", "Dune". These are creative works meant for entertainment or artistic expression.
            * *BIOGRAPHY*: A written account of a real person's life. Examples: "Steve Jobs" by Walter Isaacson, "The Diary of a Young Girl". These are non-fiction historical records of an individual.
            * *TEXTBOOK*: An educational book used for study. Examples: "Calculus: Early Transcendentals", "Introduction to Java Programming". These are designed for students and are often used as reference material in academic courses.
            * *MAGAZINE*: A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.
            * *JOURNAL*: A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.

            |`+publisher+`
            |`+object+`
            |`+body+`
            |A company that produces and sells books.

            |`+authors+`
            |`+array+`
            |`+body+`
            |The list of people who wrote this book.

            |===\
            """);

    templates
        .evaluateThat("{{GET(\"/library/books\") | request | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,1,3", options="header"]
            |===
            |Name|Type|In|Description
            |`+Accept+`
            |`+string+`
            |`+header+`
            |

            |`+title+`
            |`+string+`
            |`+query+`
            |The exact book title to filter by.

            |`+page+`
            |`+int32+`
            |`+query+`
            |Which page number to load (defaults to 1).

            |`+size+`
            |`+int32+`
            |`+query+`
            |How many books to show per page (defaults to 20).

            |===\
            """);

    templates
        .evaluateThat("{{GET(\"/library/books\") | request | parameters(query) | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,3", options="header"]
            |===
            |Name|Type|Description
            |`+title+`
            |`+string+`
            |The exact book title to filter by.

            |`+page+`
            |`+int32+`
            |Which page number to load (defaults to 1).

            |`+size+`
            |`+int32+`
            |How many books to show per page (defaults to 20).

            |===\
            """);

    templates
        .evaluateThat(
            "{{GET(\"/library/books\") | request | parameters(query, ['title']) | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,3", options="header"]
            |===
            |Name|Type|Description
            |`+title+`
            |`+string+`
            |The exact book title to filter by.

            |===\
            """);

    templates
        .evaluateThat("{{GET(\"/library/books\") | request | parameters('path') | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,3", options="header"]
            |===
            |Name|Type|Description
            |===\
            """);

    templates
        .evaluateThat("{{GET(\"/library/books\") | parameters('path') | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,3", options="header"]
            |===
            |Name|Type|Description
            |===\
            """);

    templates
        .evaluateThat("{{GET(\"/library/books\") | parameters(cookie) | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,3", options="header"]
            |===
            |Name|Description
            |===\
            """);

    templates
        .evaluateThat("{{POST(\"/library/books\") | request(body=\"none\") | http }}")
        .isEqualToIgnoringNewLines(
            """
            [source,http,options="nowrap"]
            ----
            POST /library/books HTTP/1.1
            Accept: application/json
            Content-Type: application/json
            {}
            ----\
            """);

    // example on same schema must generate same output
    templates
        .evaluateThat("{{GET(\"/library/books\") | request }}")
        .isEqualTo("GET /library/books");

    // example on same schema must generate same output
    templates
        .evaluateThat("{{GET(\"/library/books\") | request | http }}")
        .isEqualToIgnoringNewLines(
            """
            [source,http,options="nowrap"]
            ----
            GET /library/books HTTP/1.1
            Accept: application/json
            ----\
            """);

    templates
        .evaluateThat("{{POST(\"/library/books\") | request | http }}")
        .isEqualToIgnoringNewLines(
            """
            [source,http,options="nowrap"]
            ----
            POST /library/books HTTP/1.1
            Accept: application/json
            Content-Type: application/json
            {"isbn":"string","title":"string","publicationDate":"date","text":"string","type":"string","publisher":{"id":"int64","name":"string"},"authors":[{"ssn":"string","name":"string","address":{"street":"string","city":"string","zip":"string"}}]}
            ----\
            """);

    templates
        .evaluateThat("{{POST(\"/library/books\") | request | parameters(header) | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,3", options="header"]
            |===
            |Name|Description
            |`+Accept+`
            |

            |`+Content-Type+`
            |

            |===\
            """);

    templates
        .evaluateThat(
            "{{POST(\"/library/books\") | request | parameters(header) | table(columns=['name'])"
                + " }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1", options="header"]
            |===
            |Name
            |`+Accept+`

            |`+Content-Type+`

            |===\
            """);

    templates
        .evaluateThat("{{POST(\"/library/books\") | request | body | table }}")
        .isEqualToIgnoringNewLines(
            """
            [cols="1,1,3a", options="header"]
            |===
            |Name|Type|Description
            |`+isbn+`
            |`+string+`
            |The unique "barcode" for this book (ISBN). We use this to identify exactly which book edition we are talking about.

            |`+title+`
            |`+string+`
            |The name printed on the cover.

            |`+publicationDate+`
            |`+date+`
            |When this book was released to the public.

            |`+text+`
            |`+string+`
            |The full story or content of the book. Since this can be very long, we store it in a special way (Large Object) to keep the database fast.

            |`+type+`
            |`+string+`
            |Defines the format and release schedule of the item.

            * *NOVEL*: A fictional narrative story. Examples: "Pride and Prejudice", "Harry Potter", "Dune". These are creative works meant for entertainment or artistic expression.
            * *BIOGRAPHY*: A written account of a real person's life. Examples: "Steve Jobs" by Walter Isaacson, "The Diary of a Young Girl". These are non-fiction historical records of an individual.
            * *TEXTBOOK*: An educational book used for study. Examples: "Calculus: Early Transcendentals", "Introduction to Java Programming". These are designed for students and are often used as reference material in academic courses.
            * *MAGAZINE*: A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.
            * *JOURNAL*: A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.

            |`+publisher+`
            |`+object+`
            |A company that produces and sells books.

            |`+authors+`
            |`+array+`
            |The list of people who wrote this book.

            |===\
            """);
  }
}
