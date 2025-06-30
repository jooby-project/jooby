/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i3705

import io.jooby.openapi.OpenAPIResult
import io.jooby.openapi.OpenAPITest
import org.junit.jupiter.api.Assertions.assertEquals

class Issue3705 {

  @OpenAPITest(value = App3705::class)
  fun shouldParseMvcExtension(result: OpenAPIResult) {
    assertEquals(
      "openapi: 3.0.1\n" +
        "info:\n" +
        "  title: 3705 API\n" +
        "  description: 3705 API description\n" +
        "  version: \"1.0\"\n" +
        "paths:\n" +
        "  /search:\n" +
        "    get:\n" +
        "      operationId: search\n" +
        "      responses:\n" +
        "        \"200\":\n" +
        "          description: Success\n" +
        "          content:\n" +
        "            application/json:\n" +
        "              schema:\n" +
        "                type: string\n",
      result.toYaml(),
    )
  }

  @OpenAPITest(value = App3705b::class)
  fun shouldParseMvcExtensionBeanScope(result: OpenAPIResult) {
    assertEquals(
      "openapi: 3.0.1\n" +
        "info:\n" +
        "  title: 3705b API\n" +
        "  description: 3705b API description\n" +
        "  version: \"1.0\"\n" +
        "paths:\n" +
        "  /search:\n" +
        "    get:\n" +
        "      operationId: search\n" +
        "      responses:\n" +
        "        \"200\":\n" +
        "          description: Success\n" +
        "          content:\n" +
        "            application/json:\n" +
        "              schema:\n" +
        "                type: string\n",
      result.toYaml(),
    )
  }
}
