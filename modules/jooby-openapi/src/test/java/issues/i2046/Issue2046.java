package issues.i2046;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import kt.issues.i2004.App2004;

public class Issue2046 {
  @OpenAPITest(value = App2046.class)
  public void shouldSupportMountOperation(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 2046 API\n"
        + "  description: 2046 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /2046:\n"
        + "    get:\n"
        + "      operationId: get2046\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /b/2046b:\n"
        + "    get:\n"
        + "      operationId: getB2046b\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n", result.toYaml());
  }
}
