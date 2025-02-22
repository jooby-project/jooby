/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3412;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3412 {

  @OpenAPITest(value = App3412.class)
  public void shouldParseNonnullQueryParameter(OpenAPIResult result) {
    assertEquals(
        """
        openapi: 3.0.1
        info:
          title: 3412 API
          description: 3412 API description
          version: "1.0"
        paths:
          /welcome:
            get:
              operationId: sayHi
              parameters:
              - name: greeting
                in: query
                required: true
                schema:
                  type: string
              - name: language
                in: query
                schema:
                  type: string
              responses:
                "200":
                  description: Success
                  content:
                    application/json:
                      schema:
                        type: string
        """,
        result.toYaml());
  }
}
