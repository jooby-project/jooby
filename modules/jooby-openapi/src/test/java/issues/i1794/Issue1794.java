package issues.i1794;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1794 {

  @OpenAPITest(value = App1794.class)
  public void shouldGenerateOutput(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1794 API\n"
        + "  description: 1794 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /:\n"
        + "    post:\n"
        + "      summary: This is the post for testSuite\n"
        + "      description: TestSuite change1234 Description\n"
        + "      operationId: User creation\n"
        + "      requestBody:\n"
        + "        description: This is the requestBody\n"
        + "        content:\n"
        + "          application/json:\n"
        + "            schema:\n"
        + "              $ref: \"#/components/schemas/TestSuite\"\n"
        + "        required: true\n"
        + "      responses:\n"
        + "        \"204\":\n"
        + "          description: Created\n"
        + "  /{code}:\n"
        + "    get:\n"
        + "      summary: Get testSuite\n"
        + "      description: This is the description of the conducted application\n"
        + "      operationId: find\n"
        + "      parameters:\n"
        + "      - name: code\n"
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
        + "                $ref: \"#/components/schemas/TestSuite\"\n"
        + "components:\n"
        + "  schemas:\n"
        + "    TestSuite:\n"
        + "      type: object\n"
        + "      properties:\n"
        + "        foo:\n"
        + "          type: string\n", result.toYaml());
  }
}
