/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2968;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue2968 {
  @OpenAPITest(value = App2968.class)
  public void shouldParseNewControllerSourceCoe(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 2968 API\n"
            + "  description: 2968 API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /hello:\n"
            + "    get:\n"
            + "      operationId: hello\n"
            + "      parameters:\n"
            + "      - name: name\n"
            + "        in: query\n"
            + "        schema:\n"
            + "          type: string\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n",
        result.toYaml());
  }
}
