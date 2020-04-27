package issues;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import issues.i1591.App1591;
import issues.i1591.App1591b;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1591 {

  @OpenAPITest(App1591.class)
  public void shouldParseExampleFromParameters(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1591 API\n"
        + "  description: 1591 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /1591:\n"
        + "    get:\n"
        + "      summary: Example.\n"
        + "      operationId: parameterExample\n"
        + "      parameters:\n"
        + "      - name: arg1\n"
        + "        in: path\n"
        + "        description: Arg 1\n"
        + "        required: true\n"
        + "        schema:\n"
        + "          type: string\n"
        + "        example: ex1\n"
        + "      - name: arg2\n"
        + "        in: path\n"
        + "        required: true\n"
        + "        schema:\n"
        + "          type: string\n"
        + "        examples:\n"
        + "          example0:\n"
        + "            value: ex2\n"
        + "          example1:\n"
        + "            value: ex3\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "      security:\n"
        + "      - basicAuth: []\n", result.toYaml());
  }

  @OpenAPITest(App1591b.class)
  public void shouldParseSecurityRequirement(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1591b API\n"
        + "  description: 1591b API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /securityRequirements:\n"
        + "    get:\n"
        + "      operationId: securityRequirements\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "      security:\n"
        + "      - myOauth1:\n"
        + "        - \"write: read\"\n"
        + "  /securityRequirement:\n"
        + "    get:\n"
        + "      operationId: securityRequirement\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "      security:\n"
        + "      - myOauth2:\n"
        + "        - \"write: read\"\n"
        + "  /operationSecurityRequirement:\n"
        + "    get:\n"
        + "      operationId: operationSecurityRequirement\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "      security:\n"
        + "      - myOauth3:\n"
        + "        - \"write: read\"\n", result.toYaml());
  }
}
