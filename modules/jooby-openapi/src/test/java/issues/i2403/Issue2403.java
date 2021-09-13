package issues.i2403;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue2403 {
  @OpenAPITest(value = App2403.class)
  public void shouldParseMetaInf(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 2403 API\n"
        + "  description: 2403 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /copy:\n"
        + "    get:\n"
        + "      operationId: copy\n"
        + "      parameters:\n"
        + "      - name: user\n"
        + "        in: query\n"
        + "        schema:\n"
        + "          type: string\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /me:\n"
        + "    get:\n"
        + "      operationId: me\n"
        + "      parameters:\n"
        + "      - name: user\n"
        + "        in: query\n"
        + "        schema:\n"
        + "          type: string\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n", result.toYaml());
  }
}
