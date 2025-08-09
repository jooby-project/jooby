/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import examples.SecuritySchemeApp;

public class SecuritySchemeTest {

  @OpenAPITest(SecuritySchemeApp.class)
  public void shouldParseExtensionFromInfo(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: SecurityScheme API\n"
            + "  description: SecurityScheme API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /ss:\n"
            + "    get:\n"
            + "      operationId: extension\n"
            + "      parameters:\n"
            + "      - name: q\n"
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
            + "      - myOauth2Security:\n"
            + "        - \"write: read\"\n"
            + "components:\n"
            + "  securitySchemes:\n"
            + "    myOauth2Security:\n"
            + "      type: oauth2\n"
            + "      in: header\n"
            + "      flows:\n"
            + "        implicit:\n"
            + "          authorizationUrl: http://url.com/auth\n"
            + "          scopes:\n"
            + "            write:pets: modify pets in your account\n",
        result.toYaml());
  }
}
