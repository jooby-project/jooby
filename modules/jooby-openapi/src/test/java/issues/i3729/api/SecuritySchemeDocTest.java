/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class SecuritySchemeDocTest {

  @OpenAPITest(value = SecuritySchemaDocApp.class)
  public void shouldGenerateDocFromMountedApp(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: Security Scheme.\n"
            + "  description: SecuritySchemaDoc API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /pets:\n"
            + "    get:\n"
            + "      summary: Pets.\n"
            + "      operationId: getPets\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "      security:\n"
            + "      - myOauth2Security:\n"
            + "        - read:pets\n"
            + "    post:\n"
            + "      summary: Create Posts.\n"
            + "      operationId: postPets\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "      security:\n"
            + "      - myOauth2Security:\n"
            + "        - read:pets\n"
            + "        - write:pets\n"
            + "components:\n"
            + "  securitySchemes:\n"
            + "    myOauth2Security:\n"
            + "      type: oauth2\n"
            + "      name: myOauth2Security\n"
            + "      in: header\n"
            + "      flows:\n"
            + "        implicit:\n"
            + "          authorizationUrl: http://url.com/auth\n"
            + "          scopes:\n"
            + "            write:pets: modify pets in your account\n"
            + "            read:pets: read your pets\n"
            + "    myOauth:\n"
            + "      type: oauth2\n"
            + "      name: myOauth\n"
            + "      in: header\n"
            + "      flows:\n"
            + "        implicit:\n"
            + "          authorizationUrl: http://url.com/auth\n"
            + "          scopes:\n"
            + "            user:read: \"\"\n",
        result.toYaml());
  }
}
