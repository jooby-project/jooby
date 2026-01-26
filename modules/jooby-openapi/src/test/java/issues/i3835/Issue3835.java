/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3835;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3835 {

  @OpenAPITest(value = App3835.class)
  public void shouldGenerateCorrectName(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.0.1
            info:
              title: 3835 API
              description: 3835 API description
              version: "1.0"
            paths:
              /3835:
                get:
                  summary: Search/scan index.
                  operationId: search
                  parameters:
                  - name: q
                    in: query
                    description: Search string. Defaults to *
                    schema:
                      type: string
                      default: '*'
                  - name: pageSize
                    in: query
                    schema:
                      type: integer
                      format: int32
                      default: 20
                  - name: page
                    in: query
                    schema:
                      type: integer
                      format: int32
                      default: 1
                  - name: options
                    in: query
                    schema:
                      type: array
                      items:
                        type: string
                  responses:
                    "200":
                      description: Search result.
                      content:
                        application/json:
                          schema:
                            type: object
                            additionalProperties:
                              type: object\
            """);
  }

  @OpenAPITest(value = App3835Jakarta.class)
  public void shouldGenerateJakartaDefaultValues(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.0.1
            info:
              title: 3835Jakarta API
              description: 3835Jakarta API description
              version: "1.0"
            paths:
              /3835:
                get:
                  summary: Search/scan index.
                  operationId: search
                  parameters:
                  - name: q
                    in: query
                    description: Search string. Defaults to *
                    schema:
                      type: string
                      default: '*'
                  - name: pageSize
                    in: query
                    schema:
                      type: integer
                      format: int32
                      default: 20
                  - name: page
                    in: query
                    schema:
                      type: integer
                      format: int32
                      default: 1
                  - name: options
                    in: query
                    schema:
                      type: array
                      items:
                        type: string
                  responses:
                    "200":
                      description: Search result.
                      content:
                        application/json:
                          schema:
                            type: object
                            additionalProperties:
                              type: object\
            """);
  }
}
