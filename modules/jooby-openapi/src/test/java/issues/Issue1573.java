/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues;

import static org.assertj.core.api.Assertions.assertThat;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import issues.i1573.App1573;

public class Issue1573 {
  @OpenAPITest(value = App1573.class)
  public void shouldGenerateExpandPaths(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.0.1
            info:
              title: 1573 API
              description: 1573 API description
              version: "1.0"
            paths:
              /profile:
                get:
                  operationId: getProfile
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            type: string
              /profile/{id}:
                get:
                  operationId: getProfileId
                  parameters:
                  - name: id
                    in: path
                    required: true
                    schema:
                      type: string
                      default: self
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            type: string
              /c/profile:
                get:
                  operationId: profile
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            type: string
              /c/profile/{id}:
                get:
                  operationId: profile2
                  parameters:
                  - name: id
                    in: path
                    required: true
                    schema:
                      type: string
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            type: string\
            """);
  }
}
