package issues.i1905;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue1905 {

  @OpenAPITest(value = App1905.class)
  public void shouldParseInstallApp(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1905 API\n"
        + "  description: 1905 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /sub:\n"
        + "    get:\n"
        + "      operationId: getSub\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /inline/sub:\n"
        + "    get:\n"
        + "      operationId: getInlineSub\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /lambda-body/sub:\n"
        + "    get:\n"
        + "      operationId: getLambdaBodySub\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /instance-reference/sub:\n"
        + "    get:\n"
        + "      operationId: getInstanceReferenceSub\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "  /static-reference/sub:\n"
        + "    get:\n"
        + "      operationId: getStaticReferenceSub\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n", result.toYaml());
  }
}
