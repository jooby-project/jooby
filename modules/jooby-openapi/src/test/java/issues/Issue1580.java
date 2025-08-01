/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import issues.i1580.App1580;

public class Issue1580 {
  @OpenAPITest(App1580.class)
  public void shouldGenerateDefaultResponse(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 1580 API\n"
            + "  description: 1580 API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /pets/{id}:\n"
            + "    put:\n"
            + "      operationId: updatePet\n"
            + "      parameters:\n"
            + "      - name: id\n"
            + "        in: path\n"
            + "        required: true\n"
            + "        schema:\n"
            + "          type: string\n"
            + "      requestBody:\n"
            + "        content:\n"
            + "          application/json:\n"
            + "            schema:\n"
            + "              $ref: \"#/components/schemas/Data1580\"\n"
            + "        required: true\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                $ref: \"#/components/schemas/Data1580\"\n"
            + "components:\n"
            + "  schemas:\n"
            + "    Data1580:\n"
            + "      type: object\n",
        result.toYaml());
  }
}
