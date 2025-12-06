/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import static io.jooby.internal.openapi.asciidoc.OperationFilters.*;
import static io.jooby.openapi.CurrentDir.basedir;
import static io.jooby.openapi.OperationBuilder.operation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.StatusCode;
import io.jooby.internal.openapi.OpenAPIExt;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.lexer.Syntax;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import issues.i3729.api.Book;
import issues.i3729.api.BookError;

public class FilterTest {
  private static final String SERVER_URL = "https://api.libray.com";
  SnippetResolver resolver;
  private Map<String, Object> internalContext;

  @BeforeEach
  public void setup() {
    var openapi = new OpenAPIExt();
    resolver = new SnippetResolver(basedir("src", "test", "resources", "adoc"));
    internalContext =
        Map.of(
            "openapi",
            openapi,
            "serverUrl",
            SERVER_URL,
            "json",
            Json.mapper(),
            "yaml",
            Yaml.mapper(),
            "resolver",
            resolver);
    resolver.setEngine(
        new PebbleEngine.Builder()
            .extension(
                new AbstractExtension() {
                  @Override
                  @SuppressWarnings("unchecked")
                  public Map<String, Object> getGlobalVariables() {
                    var openApiRoot = Json.mapper().convertValue(openapi, Map.class);
                    openApiRoot.put("internal", internalContext);
                    return openApiRoot;
                  }
                })
            .autoEscaping(false)
            .syntax(
                new Syntax.Builder()
                    .setPrintOpenDelimiter("${")
                    .setPrintCloseDelimiter("}")
                    .setEnableNewLineTrimming(false)
                    .build())
            .build());
  }

  @Test
  public void requestParams() {
    // Query parameter
    assertThat(
            queryParameters.apply(
                operation("GET", "/api/library/{isbn}").query("foo", "bar").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            |===
            |Parameter|Type|Description

            |`+bar+`
            |`+string+`
            |

            |`+foo+`
            |`+string+`
            |

            |===\
            """);
    // Path parameter
    assertThat(
            pathParameters.apply(
                operation("GET", "/api/library/{isbn}").path("isbn").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            |===
            |Parameter|Type|Description

            |`+isbn+`
            |`+string+`
            |

            |===\
            """);

    // Cookie parameter
    assertThat(
            cookieParameters.apply(
                operation("GET", "/api/library/{isbn}").cookie("single-sign-on").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            |===
            |Parameter|Description

            |`+single-sign-on+`
            |

            |===\
            """);

    // Form
    assertThat(
            formParameters.apply(
                operation("POST", "/api/library")
                    .parameter(Map.of("form", mapOf("name", "string", "file", "binary")))
                    .consumes("multipart/form-data")
                    .build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            |===
            |Parameter|Type|Description

            |`+file+`
            |`+binary+`
            |

            |`+name+`
            |`+string+`
            |

            |===\
            """);

    // All them
    assertThat(
            requestParameters.apply(
                operation("POST", "/api/library")
                    .parameter(
                        Map.of(
                            "form",
                            mapOf("name", "string", "file", "binary"),
                            "path",
                            Map.of("isbn", "string"),
                            "query",
                            Map.of("active", "true")))
                    .consumes("multipart/form-data")
                    .build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            |===
            |Parameter|Type|Description

            |`+active+`
            |`+true+`
            |

            |`+file+`
            |`+binary+`
            |

            |`+isbn+`
            |`+string+`
            |

            |`+name+`
            |`+string+`
            |

            |===\
            """);
  }

  @Test
  public void curl() {
    assertThat(
            curl.apply(
                operation("GET", "/api/library/{isbn}").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl -X GET 'https://api.libray.com/api/library/{isbn}'
            ----\
            """);

    // Query parameter
    assertThat(
            curl.apply(
                operation("GET", "/api/library/{isbn}").query("foo", "bar").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl -X GET 'https://api.libray.com/api/library/{isbn}?foo=string&bar=string'
            ----\
            """);

    // Form parameter
    assertThat(
            curl.apply(
                operation("POST", "/api/library/{isbn}").form("foo", "bar").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl --data-urlencode 'foo=string'\\
                 --data-urlencode 'bar=string'\\
                 -X POST 'https://api.libray.com/api/library/{isbn}'
            ----\
            """);

    // Query+Form parameter
    assertThat(
            curl.apply(
                operation("POST", "/api/library/{isbn}")
                    .parameter(
                        Map.of(
                            "query",
                            mapOf("active", "boolean"),
                            "form",
                            mapOf("foo", "string", "bar", "string")))
                    .build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl --data-urlencode 'foo=string'\\
                 --data-urlencode 'bar=string'\\
                 -X POST 'https://api.libray.com/api/library/{isbn}?active=boolean'
            ----\
            """);

    // Passing arguments
    assertThat(
            curl.apply(
                operation("GET", "/api/library/{isbn}").build(),
                args("-i"),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl -i\\
                 -X GET 'https://api.libray.com/api/library/{isbn}'
            ----\
            """);

    // Override method
    assertThat(
            curl.apply(
                operation("GET", "/api/library/{isbn}").build(),
                args("-i", "-X", "POST"),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl -i\\
                 -X POST 'https://api.libray.com/api/library/{isbn}'
            ----\
            """);

    // With Accept Header
    assertThat(
            curl.apply(
                operation("GET", "/api/library/{isbn}").produces("application/json").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl -H 'Accept: application/json'\\
                 -X GET 'https://api.libray.com/api/library/{isbn}'
            ----\
            """);

    // With Override Accept Header
    assertThat(
            curl.apply(
                operation("GET", "/api/library/{isbn}").produces("application/json").build(),
                args("-H", "'Accept: application/xml'"),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl -H 'Accept: application/xml'\\
                 -X GET 'https://api.libray.com/api/library/{isbn}'
            ----\
            """);

    assertThat(
            curl.apply(
                operation("POST", "/api/library").body(new Book(), "application/json").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl -H 'Content-Type: application/json'\\
                 -d '{"isbn":"string","title":"string","publicationDate":"date","text":"string","type":"string","authors":[],"image":"binary"}'\\
                 -X POST 'https://api.libray.com/api/library'
            ----\
            """);

    assertThat(
            curl.apply(
                operation("POST", "/api/library")
                    .parameter(Map.of("form", mapOf("name", "string", "file", "binary")))
                    .consumes("multipart/form-data")
                    .build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,bash]
            ----
            curl -H 'Content-Type: multipart/form-data'\\
                 --data-urlencode 'name=string'\\
                 -F "file=@/file.extension"\\
                 -X POST 'https://api.libray.com/api/library'
            ----\
            """);
  }

  @Test
  public void httpRequest() {
    assertThat(
            request.apply(
                operation("GET", "/api/library/{isbn}").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,http,options="nowrap"]
            ----
            GET /api/library/{isbn} HTTP/1.1
            ----\
            """);

    assertThat(
            request.apply(
                operation("GET", "/api/library/{isbn}").produces("application/json").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,http,options="nowrap"]
            ----
            GET /api/library/{isbn} HTTP/1.1
            Accept: application/json
            ----\
            """);

    assertThat(
            request.apply(
                operation("POST", "/api/library").body(new Book(), "application/json").build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,http,options="nowrap"]
            ----
            POST /api/library HTTP/1.1
            Content-Type: application/json
            {"isbn":"string","title":"string","publicationDate":"date","text":"string","type":"string","authors":[],"image":"binary"}
            ----\
            """);
  }

  @Test
  public void httpResponse() {
    assertThat(
            response.apply(
                operation("GET", "/api/library/{isbn}").defaultResponse().build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,http,options="nowrap"]
            ----
            HTTP/1.1 200 Success
            ----\
            """);

    assertThat(
            response.apply(
                operation("GET", "/api/library/{isbn}")
                    .defaultResponse()
                    .produces("application/json")
                    .build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,http,options="nowrap"]
            ----
            HTTP/1.1 200 Success
            Content-Type: application/json
            ----\
            """);

    assertThat(
            response.apply(
                operation("POST", "/api/library")
                    .produces("application/json")
                    .response(new Book(), StatusCode.CREATED, "application/json")
                    .build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,http,options="nowrap"]
            ----
            HTTP/1.1 201 Created
            Content-Type: application/json
            {"isbn":"string","title":"string","publicationDate":"date","text":"string","type":"string","authors":[],"image":"binary"}
            ----\
            """);

    assertThat(
            response.apply(
                operation("POST", "/api/library")
                    .produces("application/json")
                    .response(new Book(), StatusCode.CREATED, "application/json")
                    .build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,http,options="nowrap"]
            ----
            HTTP/1.1 201 Created
            Content-Type: application/json
            {"isbn":"string","title":"string","publicationDate":"date","text":"string","type":"string","authors":[],"image":"binary"}
            ----\
            """);

    assertThat(
            response.apply(
                operation("POST", "/api/library")
                    .produces("application/json")
                    .response(new Book(), StatusCode.CREATED, "application/json")
                    .response(new BookError(), StatusCode.BAD_REQUEST, "application/json")
                    .build(),
                args("400"),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            [source,http,options="nowrap"]
            ----
            HTTP/1.1 400 Bad Request
            Content-Type: application/json
            {"path":"string","message":"string","code":"int32"}
            ----\
            """);
  }

  @Test
  public void responseFields() {
    assertThat(
            responseFields.apply(
                operation("POST", "/api/library")
                    .produces("application/json")
                    .response(new Book(), StatusCode.CREATED, "application/json")
                    .build(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            |===
            |Path|Type|Description

            |`+isbn+`
            |`+string+`
            |

            |`+title+`
            |`+string+`
            |

            |`+publicationDate+`
            |`+date+`
            |

            |`+text+`
            |`+string+`
            |

            |`+type+`
            |`+string+`
            |

            |`+authors+`
            |`+[]+`
            |

            |`+image+`
            |`+binary+`
            |

            |===\
            """);

    assertEquals(
        """
        |===
        |Path|Type|Description

        |`+isbn+`
        |`+string+`
        |

        |`+title+`
        |`+string+`
        |

        |`+publicationDate+`
        |`+date+`
        |

        |`+text+`
        |`+string+`
        |

        |`+type+`
        |`+string+`
        |

        |`+authors+`
        |`+[]+`
        |

        |`+image+`
        |`+binary+`
        |

        |===\
        """,
        responseFields.apply(
            operation("POST", "/api/library")
                .produces("application/json")
                .response(new Book(), StatusCode.CREATED, "application/json")
                .build(),
            args(),
            template(),
            evaluationContext(),
            1));

    assertThat(
            responseFields.apply(
                operation("POST", "/api/library")
                    .produces("application/json")
                    .response(new Book(), StatusCode.CREATED, "application/json")
                    .response(new BookError(), StatusCode.BAD_REQUEST, "application/json")
                    .build(),
                args("400"),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            |===
            |Path|Type|Description

            |`+path+`
            |`+string+`
            |

            |`+message+`
            |`+string+`
            |

            |`+code+`
            |`+int32+`
            |

            |===\
            """);
  }

  @Test
  public void statusCode() {
    assertThat(
            statusCode.apply(
                operation("POST", "/api/library")
                    .produces("application/json")
                    .response(new Book(), StatusCode.CREATED, "application/json")
                    .response(new BookError(), StatusCode.BAD_REQUEST, "application/json")
                    .build(),
                args("201"),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            .Created
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
            ----\
            """);

    assertThat(
            statusCode.apply(
                operation("POST", "/api/library")
                    .produces("application/json")
                    .response(new Book(), StatusCode.CREATED, "application/json")
                    .response(new BookError(), StatusCode.BAD_REQUEST, "application/json")
                    .build(),
                args("400"),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
            .Bad Request
            [source,json]
            ----
            {
              "path" : "string",
              "message" : "string",
              "code" : "int32"
            }
            ----\
            """);
  }

  @Test
  public void schema() {
    // Request Body
    assertThat(
            schema.apply(
                operation("POST", "/api/library")
                    .body(new Book(), "application/json")
                    .build()
                    .getRequestBody(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
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
            ----\
            """);

    // Response
    assertThat(
            schema.apply(
                operation("POST", "/api/library")
                    .response(new Book(), StatusCode.OK, "application/json")
                    .build()
                    .getDefaultResponse(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
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
            ----\
            """);

    // Schema
    assertThat(
            schema.apply(
                operation("POST", "/api/library")
                    .response(new Book(), StatusCode.OK, "application/json")
                    .build()
                    .getDefaultResponse()
                    .getContent()
                    .get("application/json")
                    .getSchema(),
                args(),
                template(),
                evaluationContext(),
                1))
        .isEqualToNormalizingNewlines(
            """
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
            ----\
            """);
  }

  private Map<String, Object> args(Object... args) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < args.length; i++) {
      result.put(Integer.toString(i), args[i]);
    }
    return result;
  }

  private static Map<String, String> mapOf(String... values) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;
  }

  private EvaluationContext evaluationContext() {
    var context = mock(EvaluationContext.class);
    when(context.getVariable("internal")).thenReturn(internalContext);
    return context;
  }

  private PebbleTemplate template() {
    return mock(PebbleTemplate.class);
  }
}
