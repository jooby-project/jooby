/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3952;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import io.swagger.v3.oas.models.SpecVersion;

public class Issue3952 {
  @OpenAPITest(value = App3952.class, version = SpecVersion.V31)
  public void shouldParseNestedPath(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.1.0
            info:
              title: 3952 API
              description: 3952 API description
              version: "1.0"
            paths:
              /api/getThing:
                post:
                  summary: Get a thing
                  operationId: getThing
                  parameters:
                  - name: x-api-key
                    in: header
                    description: API Key
                    required: true
                    schema:
                      type: string
                  - name: x-bool
                    in: header
                    description: Boolean key
                    schema:
                      type: boolean
                  - name: x-number
                    in: header
                    description: Number key
                    schema:
                      type: number
                  - name: x-integer
                    in: header
                    description: Int key
                    required: true
                    schema:
                      type: integer
                      format: int32
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            type: string
            """);
  }
}
