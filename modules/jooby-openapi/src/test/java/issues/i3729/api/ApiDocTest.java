/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.CurrentDir;
import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class ApiDocTest {

  @OpenAPITest(value = AppLibrary.class)
  public void shouldGenerateMvcDoc(OpenAPIResult result) {
    checkResult(result);
  }

  @OpenAPITest(value = AppDemoLibrary.class)
  public void shouldGenerateGoodDoc(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.0.1
            info:
              title: DemoLibrary API
              description: DemoLibrary API description
              version: "1.0"
            paths:
              /library/books/{isbn}:
                summary: The Public Front Desk of the library.
                get:
                  tags:
                  - Library
                  summary: Get Specific Book Details
                  description: View the full information for a single specific book using its
                    unique ISBN.
                  operationId: getBook
                  parameters:
                  - name: isbn
                    in: path
                    description: "The unique ID from the URL (e.g., /books/978-3-16-148410-0)"
                    required: true
                    schema:
                      type: string
                  responses:
                    "200":
                      description: The book data
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/Book"
                    "404":
                      description: "Not Found: error if it doesn't exist."
              /library/search:
                summary: The Public Front Desk of the library.
                get:
                  tags:
                  - Library
                  summary: Quick Search
                  description: "Find books by a partial title (e.g., searching \\"Harry\\" finds\\
                    \\ \\"Harry Potter\\")."
                  operationId: searchBooks
                  parameters:
                  - name: q
                    in: query
                    description: The word or phrase to search for.
                    schema:
                      type: string
                  responses:
                    "200":
                      description: A list of books matching that term.
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
              /library/books:
                summary: The Public Front Desk of the library.
                get:
                  tags:
                  - Library
                  summary: Browse Books (Paginated)
                  description: "Look up a specific book title where there might be many editions\\
                    \\ or copies, splitting the results into manageable pages."
                  operationId: getBooksByTitle
                  parameters:
                  - name: title
                    in: query
                    description: The exact book title to filter by.
                    schema:
                      type: string
                  - name: page
                    in: query
                    description: Which page number to load (defaults to 1).
                    required: true
                    schema:
                      type: integer
                      format: int32
                  - name: size
                    in: query
                    description: How many books to show per page (defaults to 20).
                    required: true
                    schema:
                      type: integer
                      format: int32
                  responses:
                    "200":
                      description: "A \\"Page\\" object containing the books and info like \\"Total\\
                        \\ Pages: 5\\"."
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              content:
                                type: array
                                items:
                                  $ref: "#/components/schemas/Book"
                              numberOfElements:
                                type: integer
                                format: int32
                              totalElements:
                                type: integer
                                format: int64
                              totalPages:
                                type: integer
                                format: int64
                              pageRequest:
                                type: object
                                properties:
                                  page:
                                    type: integer
                                    format: int64
                                  size:
                                    type: integer
                                    format: int32
                              nextPageRequest:
                                type: object
                                properties:
                                  page:
                                    type: integer
                                    format: int64
                                  size:
                                    type: integer
                                    format: int32
                              previousPageRequest:
                                type: object
                                properties:
                                  page:
                                    type: integer
                                    format: int64
                                  size:
                                    type: integer
                                    format: int32
                post:
                  tags:
                  - Inventory
                  summary: Add New Book
                  description: Register a new book in the system.
                  operationId: addBook
                  requestBody:
                    description: New book to add.
                    content:
                      application/json:
                        schema:
                          $ref: "#/components/schemas/Book"
                    required: true
                  responses:
                    "200":
                      description: A text message confirming success.
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/Book"
            components:
              schemas:
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
                        $ref: "#/components/schemas/Book"\
            """);
  }

  @OpenAPITest(value = AppLibrary.class)
  public void shouldGenerateAdoc(OpenAPIResult result) {
    assertThat(
            result.toAsciiDoc(
                CurrentDir.basedir("src", "test", "resources", "adoc", "library.adoc")))
        .isEqualToIgnoringNewLines(
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

            Example: `/api/library?title=...`

            ==== Request Fields

            [cols="1,1,3", options="header"]
            |===
            |Name|Type|Description
            |`+title+`
            |`+string+`
            |Book's title.

            |`+author+`
            |`+string+`
            |Book's author. Optional.

            |`+isbn+`
            |`+array+`
            |Book's isbn. Optional.

            |===

            === Find a book by ISBN

            [source]
            ----
            curl -i\\
                 -H 'Accept: application/json'\\
                 -X GET '/api/library/{isbn}'
            ----

            .GET /api/library/{isbn}
            [source, json]
            ----
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
            }
            ----

            .GET /api/library/{isbn}
            [source, json]
            ----
            {
              "message" : "Bad Request: For bad ISBN code.",
              "reason" : "Bad Request",
              "statusCode" : 400
            }
            ----

            .GET /api/library/{isbn}
            [source, json]
            ----
            {
              "message" : "Not Found: If a book doesn't exist.",
              "reason" : "Not Found",
              "statusCode" : 404
            }
            ----

            ==== Response Fields

            [cols="1,1,3a", options="header"]
            |===
            |Name|Type|Description
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
            |Books can be broadly categorized into fiction and non-fiction.

            * *Fiction*: Fiction books are based on imaginary characters and events, while non-fiction books are based o n real people and events.
            * *NonFiction*: Non-fiction genres include biography, autobiography, history, self-help, and true crime.

            |`+authors+`
            |`+array+`
            |

            |`+image+`
            |`+binary+`
            |

            |===\
            """);
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
