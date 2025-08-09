/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3652;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3652 {

  @OpenAPITest(value = App3652.class)
  public void shouldGenerateSecuritySchemeFromController(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 3652 API\n"
            + "  description: 3652 API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /3652/{id}:\n"
            + "    get:\n"
            + "      summary: Find a user by ID\n"
            + "      description: Finds a user by ID or throws a 404\n"
            + "      operationId: sayHi\n"
            + "      parameters:\n"
            + "      - name: id\n"
            + "        in: path\n"
            + "        required: true\n"
            + "        schema:\n"
            + "          type: string\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "      security:\n"
            + "      - myBearerToken:\n"
            + "        - user:read\n"
            + "components:\n"
            + "  securitySchemes:\n"
            + "    myBearerToken:\n"
            + "      type: http\n"
            + "      in: header\n"
            + "      scheme: bearer\n"
            + "      bearerFormat: JWT\n",
        result.toYaml());
  }
}
