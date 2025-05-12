/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3654;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3654 {

  @OpenAPITest(value = App3654.class)
  public void shouldGenerateParamDocInRightOrder(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 3654 API\n"
            + "  description: 3654 API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /3652/{id}:\n"
            + "    get:\n"
            + "      summary: Find a user by ID\n"
            + "      description: Finds a user by ID or throws a 404\n"
            + "      operationId: getUser\n"
            + "      parameters:\n"
            + "      - name: id\n"
            + "        in: path\n"
            + "        description: The user ID\n"
            + "        required: true\n"
            + "        schema:\n"
            + "          type: string\n"
            + "      - name: activeOnly\n"
            + "        in: query\n"
            + "        description: Flag for fetching active/inactive users. (Defaults to true if\n"
            + "          not provided)\n"
            + "        schema:\n"
            + "          type: boolean\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: object\n"
            + "                additionalProperties:\n"
            + "                  type: object\n",
        result.toYaml());
  }
}
