package issues.i3412;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3412 {

  @OpenAPITest(value = App3412.class)
  public void shouldNonnullQueryParameter(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 3412 API\n"
            + "  description: 3412 API description\n"
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
