package issues;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import issues.i1768.App1768;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1768 {
  @OpenAPITest(value = App1768.class)
  public void shouldIgnoreHiddenOperations(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n" +
          "info:\n" +
          "  title: 1768 API\n" +
          "  description: 1768 API description\n" +
          "  version: \"1.0\"\n" +
          "paths:\n" +
          "  /c/not-hidden:\n" +
          "    get:\n" +
          "      operationId: notHidden\n" +
          "      responses:\n" +
          "        \"200\":\n" +
          "          description: Success\n", result.toYaml());
  }
}
