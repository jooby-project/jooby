/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3230;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import kt.i3230.App3230Kt;

public class Issue3230 {

  @OpenAPITest(value = App3230Kt.class)
  public void shouldSupportExtensionMethod(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 3230 API\n"
            + "  description: 3230 API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /hello:\n"
            + "    get:\n"
            + "      operationId: getHello\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "  /create:\n"
            + "    post:\n"
            + "      operationId: postCreate\n"
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
