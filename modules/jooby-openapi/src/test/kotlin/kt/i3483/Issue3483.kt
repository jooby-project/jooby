/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i3483

import io.jooby.openapi.OpenAPIResult
import io.jooby.openapi.OpenAPITest
import org.junit.jupiter.api.Assertions.assertEquals

class Issue3483 {

  @OpenAPITest(value = App3483::class)
  fun shouldFindAnonymousObject(result: OpenAPIResult) {
    assertEquals(
      "openapi: 3.0.1\n" +
        "info:\n" +
        "  title: 3483 API\n" +
        "  description: 3483 API description\n" +
        "  version: \"1.0\"\n" +
        "paths:\n" +
        "  /some/{*}:\n" +
        "    get:\n" +
        "      operationId: getSome\n" +
        "      parameters:\n" +
        "      - name: '*'\n" +
        "        in: path\n" +
        "        required: true\n" +
        "        schema:\n" +
        "          pattern: \\.*\n" +
        "          type: string\n" +
        "      responses:\n" +
        "        \"200\":\n" +
        "          description: Success\n" +
        "          content:\n" +
        "            application/json:\n" +
        "              schema:\n" +
        "                type: string\n" +
        "  /named-unused/{pathparam}:\n" +
        "    get:\n" +
        "      operationId: getNamedUnusedPathparam\n" +
        "      parameters:\n" +
        "      - name: pathparam\n" +
        "        in: path\n" +
        "        required: true\n" +
        "        schema:\n" +
        "          pattern: \\.*\n" +
        "          type: string\n" +
        "      responses:\n" +
        "        \"200\":\n" +
        "          description: Success\n" +
        "          content:\n" +
        "            application/json:\n" +
        "              schema:\n" +
        "                type: string\n" +
        "  /here:\n" +
        "    get:\n" +
        "      operationId: getHere\n" +
        "      responses:\n" +
        "        \"200\":\n" +
        "          description: Success\n" +
        "          content:\n" +
        "            application/json:\n" +
        "              schema:\n" +
        "                type: string\n" +
        "  /api/foo:\n" +
        "    get:\n" +
        "      operationId: getApiFoo\n" +
        "      responses:\n" +
        "        \"200\":\n" +
        "          description: Success\n" +
        "          content:\n" +
        "            application/json:\n" +
        "              schema:\n" +
        "                type: string\n" +
        "  /foo:\n" +
        "    get:\n" +
        "      operationId: getFoo\n" +
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
