package issues;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1586 {

  @OpenAPITest(App1586.class)
  public void shouldParseSubClass(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1586 API\n"
        + "  description: 1586 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /base:\n"
        + "    get:\n"
        + "      operationId: base\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /base/withPath:\n"
        + "    get:\n"
        + "      operationId: withPath\n"
        + "      parameters:\n"
        + "      - name: q\n"
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
        + "  /base/subPath:\n"
        + "    get:\n"
        + "      operationId: subPath\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n", result.toYaml());
  }

  @OpenAPITest(App1586b.class)
  public void shouldOverridePathOnSubClass(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1586b API\n"
        + "  description: 1586b API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /override:\n"
        + "    get:\n"
        + "      operationId: base\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /override/withPath:\n"
        + "    get:\n"
        + "      operationId: withPath\n"
        + "      parameters:\n"
        + "      - name: q\n"
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

  @OpenAPITest(App1586c.class)
  public void shouldOverrideMethodOnSubClass(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1586c API\n"
        + "  description: 1586c API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /overrideMethod:\n"
        + "    get:\n"
        + "      operationId: base\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /overrideMethod/newpath:\n"
        + "    get:\n"
        + "      operationId: withPath\n"
        + "      parameters:\n"
        + "      - name: q\n"
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
