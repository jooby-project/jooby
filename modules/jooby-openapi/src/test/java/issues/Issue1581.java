package issues;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import issues.i1581.App1581;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1581 {
  @OpenAPITest(value = App1581.class)
  public void shouldGenerateDefaultResponse(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1581 API\n"
        + "  description: 1581 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /pets/{id}:\n"
        + "    put:\n"
        + "      operationId: updatePet\n"
        + "      parameters:\n"
        + "      - name: id\n"
        + "        in: path\n"
        + "        required: true\n"
        + "        schema:\n"
        + "          type: string\n"
        + "      requestBody:\n"
        + "        content:\n"
        + "          application/json:\n"
        + "            schema:\n"
        + "              $ref: \"#/components/schemas/Data1580\"\n"
        + "        required: false\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                $ref: \"#/components/schemas/Data1580\"\n"
        + "components:\n"
        + "  schemas:\n"
        + "    Data1580:\n"
        + "      type: object\n", result.toYaml());
  }
}
