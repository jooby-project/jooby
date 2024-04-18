package issues.i3397;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3397 {

  @OpenAPITest(value = App3397.class)
  public void shouldParseAvajeBeanScopeControllers(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 3397 API\n"
            + "  description: 3397 API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /welcome:\n"
            + "    get:\n"
            + "      operationId: sayHi\n"
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
