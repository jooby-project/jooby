package issues.i2505;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue2505 {
  @OpenAPITest(value = App2505.class)
  public void shouldParseMapValues(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 2505 API\n"
        + "  description: 2505 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /2505:\n"
        + "    get:\n"
        + "      operationId: mapWithStringValue\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: object\n"
        + "                additionalProperties:\n"
        + "                  type: string\n"
        + "  /2505/value:\n"
        + "    get:\n"
        + "      operationId: mapWithCustomValue\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: object\n"
        + "                additionalProperties:\n"
        + "                  $ref: '#/components/schemas/Value2505'\n"
        + "  /2505/arrayValue:\n"
        + "    get:\n"
        + "      operationId: mapWithCustomArrayValue\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: object\n"
        + "                additionalProperties:\n"
        + "                  type: array\n"
        + "                  items:\n"
        + "                    $ref: '#/components/schemas/Value2505'\n"
        + "components:\n"
        + "  schemas:\n"
        + "    Value2505:\n"
        + "      type: object\n"
        + "      properties:\n"
        + "        code:\n"
        + "          type: integer\n"
        + "          format: int32\n"
        + "        text:\n"
        + "          type: string\n", result.toYaml());
  }
}
