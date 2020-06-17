package issues.i1795;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1795 {

  @OpenAPITest(value = App1795.class)
  public void shouldGetRequestBody(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1795 API\n"
        + "  description: 1795 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /param:\n"
        + "    post:\n"
        + "      operationId: create\n"
        + "      requestBody:\n"
        + "        description: String list\n"
        + "        content:\n"
        + "          application/json:\n"
        + "            schema:\n"
        + "              type: array\n"
        + "              items:\n"
        + "                type: string\n"
        + "        required: true\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: array\n"
        + "                items:\n"
        + "                  type: string\n"
        + "  /method:\n"
        + "    post:\n"
        + "      operationId: createAtMethod\n"
        + "      requestBody:\n"
        + "        description: At method level list\n"
        + "        content:\n"
        + "          application/json:\n"
        + "            schema:\n"
        + "              type: array\n"
        + "              items:\n"
        + "                type: string\n"
        + "        required: false\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: array\n"
        + "                items:\n"
        + "                  type: string\n", result.toYaml());
  }
}
