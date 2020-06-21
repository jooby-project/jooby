package issues.i1805;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1805 {

  @OpenAPITest(value = App1805.class)
  public void shouldGenerateURIURLParam(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1805 API\n"
        + "  description: 1805 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /1805/uri:\n"
        + "    get:\n"
        + "      operationId: uri\n"
        + "      parameters:\n"
        + "      - name: param\n"
        + "        in: query\n"
        + "        schema:\n"
        + "          type: string\n"
        + "          format: uri\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "                format: uri\n"
        + "  /1805/url:\n"
        + "    get:\n"
        + "      operationId: url\n"
        + "      parameters:\n"
        + "      - name: param\n"
        + "        in: query\n"
        + "        schema:\n"
        + "          type: string\n"
        + "          format: url\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "                format: url\n", result.toYaml());
  }
}
