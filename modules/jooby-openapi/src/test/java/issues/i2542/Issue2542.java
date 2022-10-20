/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2542;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue2542 {
  @OpenAPITest(value = App2542.class)
  public void shouldParseMapValues(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 2542 API\n"
            + "  description: 2542 API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /2542:\n"
            + "    get:\n"
            + "      operationId: byteArray\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "                format: byte\n"
            + "  /2542/annotation:\n"
            + "    get:\n"
            + "      operationId: byteArrayWithAnnotation\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "                format: byte\n"
            + "                nullable: true\n",
        result.toYaml());
  }
}
