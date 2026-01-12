/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3841;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3841 {
  @OpenAPITest(value = App3841.class)
  public void shouldGenerateDoc(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.0.1
            info:
              title: App with or without doc.
              description: Description doc.
              version: "1.0"
            paths:
              /3841:
                summary: Paths.
                get:
                  summary: Hello endpoint.
                  operationId: hello
                  parameters:
                  - name: name
                    in: query
                    description: Name arg.
                    schema:
                      type: string
                  responses:
                    "200":
                      description: Hello endpoint.
                      content:
                        application/json:
                          schema:
                            type: string\
            """);
  }

  @OpenAPITest(value = App3841.class, javadoc = "off")
  public void shouldNotGenerateDoc(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.0.1
            info:
              title: 3841 API
              description: 3841 API description
              version: "1.0"
            paths:
              /3841:
                get:
                  operationId: hello
                  parameters:
                  - name: name
                    in: query
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
