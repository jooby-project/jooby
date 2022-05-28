package issues.i2594;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue2594 {

  @OpenAPITest(value = App2594.class)
  public void shouldNotDuplicateMountedApp(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 2594 API\n"
        + "  description: 2594 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /healthcheck:\n"
        + "    get:\n"
        + "      operationId: healthCheck\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "  /api/v1/welcome:\n"
        + "    get:\n"
        + "      operationId: sayHi\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /api/v1/should-not-be-duplicated-under-v2:\n"
        + "    get:\n"
        + "      operationId: demo\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /api/v2/welcome:\n"
        + "    get:\n"
        + "      operationId: sayHi2\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n", result.toYaml());
  }
}
