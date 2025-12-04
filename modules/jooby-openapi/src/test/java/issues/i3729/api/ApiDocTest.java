/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.CurrentDir;
import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class ApiDocTest {

  @OpenAPITest(value = AppLibrary.class)
  public void shouldGenerateMvcDoc(OpenAPIResult result) {
    checkResult(result);
  }

  @OpenAPITest(value = AppLibrary.class)
  public void shouldGenerateAdoc(OpenAPIResult result) {
    assertEquals(
        """
        = Library API.
        Jooby Doc;
        :doctype: book
        :icons: font
        :source-highlighter: highlightjs
        :toc: left
        :toclevels: 4
        :sectlinks:

        == Introduction

        Available data: Books and authors.

        == Support

        Write your questions at support@jooby.io

        [[overview_operations]]
        == Operations

        === List Books

        Query books. By using advanced filters.

        [source,bash]
        ----
        curl -H 'Accept: application/json'\\
             -X GET 'https://api.fake-museum-example.com/v1/api/library?title=string&author=string&isbn=string1&isbn=string2&isbn=string3'
        ----

        ==== Request Fields

        |===
        |Parameter|Type|Description

        |`+author+`
        |`+string+`
        |Book's author. Optional.

        |`+isbn+`
        |`+array+`
        |Book's isbn. Optional.

        |`+title+`
        |`+string+`
        |Book's title.

        |===

        === Find a book by ISBN

        [source,bash]
        ----
        curl -i\\
             -H 'Accept: application/json'\\
             -X GET 'https://api.fake-museum-example.com/v1/api/library/{isbn}'
        ----

        .A matching book.
        [source,json]
        ----
        {
          "isbn" : "string",
          "title" : "string",
          "publicationDate" : "date",
          "text" : "string",
          "type" : "string",
          "authors" : [ ],
          "image" : "binary"
        }
        ----

        .Bad Request: For bad ISBN code.
        [source,json]
        ----
        {
          "message" : "...",
          "statusCode" : 400,
          "reason" : "Bad Request"
        }
        ----

        .Not Found: If a book doesn't exist.
        [source,json]
        ----
        {
          "message" : "...",
          "statusCode" : 404,
          "reason" : "Not Found"
        }
        ----

        ==== Response Fields

        |===
        |Path|Type|Description

        |`+isbn+`
        |`+string+`
        |Book ISBN. Method.

        |`+title+`
        |`+string+`
        |Book's title.

        |`+publicationDate+`
        |`+date+`
        |Publication date. Format mm-dd-yyyy.

        |`+text+`
        |`+string+`
        |Book's content.

        |`+type+`
        |`+string+`
        |Book type.
          - Fiction: Fiction books are based on imaginary characters and events, while non-fiction books are based o n real people and events.
          - NonFiction: Non-fiction genres include biography, autobiography, history, self-help, and true crime.

        |`+authors+`
        |`+[]+`
        |

        |`+image+`
        |`+binary+`
        |

        |===
        """
            .trim(),
        result.toAsciiDoc(CurrentDir.basedir("src", "test", "resources", "adoc", "library.adoc")));
  }

  @OpenAPITest(value = ScriptLibrary.class)
  public void shouldGenerateScriptDoc(OpenAPIResult result) {
    checkResult(result);
  }

  private void checkResult(OpenAPIResult result) {
    assertEquals(
        """
        openapi: 3.0.1
        info:
          title: Library API.
          description: "Available data: Books and authors."
          contact:
            name: Jooby
            url: https://jooby.io
            email: support@jooby.io
          license:
            name: Apache
            url: https://jooby.io/LICENSE
          version: 4.0.0
          x-logo:
            url: https://redocly.github.io/redoc/museum-logo.png
            altText: Museum logo
        servers:
        - url: https://api.fake-museum-example.com/v1
        tags:
        - name: Library
          description: Access to all books.
        - name: Author
          description: Oxxx
        paths:
          /api/library/{isbn}:
            summary: Library API.
            description: "Contains all operations for creating, updating and fetching books."
            get:
              tags:
              - Library
              - Book
              - Author
              summary: Find a book by isbn.
              operationId: bookByIsbn
              parameters:
              - name: isbn
                in: path
                description: Book isbn. Like IK-1900.
                required: true
                schema:
                  type: string
              responses:
                "200":
                  description: A matching book.
                  content:
                    application/json:
                      schema:
                        $ref: "#/components/schemas/Book"
                "404":
                  description: "Not Found: If a book doesn't exist."
                "400":
                  description: "Bad Request: For bad ISBN code."
          /api/library/author/{id}:
            summary: Library API.
            description: "Contains all operations for creating, updating and fetching books."
            get:
              tags:
              - Library
              - Author
              summary: Author by Id.
              operationId: author
              parameters:
              - name: id
                in: path
                description: Author ID.
                required: true
                schema:
                  type: string
              responses:
                "200":
                  description: An author
                  content:
                    application/json:
                      schema:
                        $ref: "#/components/schemas/Author"
          /api/library:
            summary: Library API.
            description: "Contains all operations for creating, updating and fetching books."
            get:
              tags:
              - Library
              summary: Query books.
              description: By using advanced filters.
              operationId: query
              parameters:
              - name: title
                in: query
                description: Book's title.
                schema:
                  type: string
              - name: author
                in: query
                description: Book's author. Optional.
                schema:
                  type: string
              - name: isbn
                in: query
                description: Book's isbn. Optional.
                schema:
                  type: array
                  items:
                    type: string
              responses:
                "200":
                  description: Matching books.
                  content:
                    application/json:
                      schema:
                        type: array
                        items:
                          $ref: "#/components/schemas/Book"
              x-badges:
              - name: Beta
                position: before
                color: purple
            post:
              tags:
              - Library
              - Author
              summary: Creates a new book.
              description: Book can be created or updated.
              operationId: createBook
              requestBody:
                description: Book to create.
                content:
                  application/json:
                    schema:
                      $ref: "#/components/schemas/Book"
                    example:
                      isbn: X01981
                      title: HarryPotter
                required: true
              responses:
                "200":
                  description: Saved book.
                  content:
                    application/json:
                      schema:
                        $ref: "#/components/schemas/Book"
                      example:
                        id: generatedId
                        isbn: '...'
        components:
          schemas:
            Author:
              type: object
              properties:
                ssn:
                  type: string
                  description: Social security number.
                name:
                  type: string
                  description: Author's name.
                address:
                  $ref: "#/components/schemas/Address"
                books:
                  uniqueItems: true
                  type: array
                  description: Published books.
                  items:
                    $ref: "#/components/schemas/Book"
            BookQuery:
              type: object
              properties:
                title:
                  type: string
                  description: Book's title.
                author:
                  type: string
                  description: Book's author. Optional.
                isbn:
                  type: array
                  description: Book's isbn. Optional.
                  items:
                    type: string
              description: Query books by complex filters.
            Address:
              type: object
              properties:
                street:
                  type: string
                  description: Street name.
                city:
                  type: string
                  description: City name.
                state:
                  type: string
                  description: State.
                country:
                  type: string
                  description: Two digit country code.
              description: Author address.
            Book:
              type: object
              properties:
                isbn:
                  type: string
                  description: Book ISBN. Method.
                title:
                  type: string
                  description: Book's title.
                publicationDate:
                  type: string
                  description: Publication date. Format mm-dd-yyyy.
                  format: date
                text:
                  type: string
                  description: Book's content.
                type:
                  type: string
                  description: |-
                    Book type.
                      - Fiction: Fiction books are based on imaginary characters and events, while non-fiction books are based o n real people and events.
                      - NonFiction: Non-fiction genres include biography, autobiography, history, self-help, and true crime.
                  enum:
                  - Fiction
                  - NonFiction
                authors:
                  uniqueItems: true
                  type: array
                  items:
                    $ref: "#/components/schemas/Author"
                image:
                  type: string
                  format: binary
              description: Book model.
        """,
        result.toYaml());
  }
}
