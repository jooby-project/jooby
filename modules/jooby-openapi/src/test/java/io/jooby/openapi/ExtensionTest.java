package io.jooby.openapi;

import examples.ExtensionApp;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExtensionTest {

  @OpenAPITest(ExtensionApp.class)
  public void shouldParseExtensionFromInfo(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: Extension API\n"
        + "  description: Extension API description\n"
        + "  version: \"1.0\"\n"
        + "  x-api-json:\n"
        + "    properties:\n"
        + "      enabled: true\n"
        + "tags:\n"
        + "- name: tag\n"
        + "  x-tag:\n"
        + "    properties:\n"
        + "      value: 45\n"
        + "paths:\n"
        + "  /op/{q}:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - tag\n"
        + "      operationId: extension\n"
        + "      parameters:\n"
        + "      - name: q\n"
        + "        in: path\n"
        + "        required: true\n"
        + "        schema:\n"
        + "          type: string\n"
        + "        x-q: ext\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "          x-rsp: ext\n"
        + "      x-x: \"y\"\n"
        + "      x-y: z\n", result.toYaml());
  }
}
