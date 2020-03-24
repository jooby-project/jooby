package issues;

import io.jooby.openapi.DebugOption;
import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import issues.i1592.App1592;
import issues.i1596.PathOperatorWithTags;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1596 {

  @OpenAPITest(value = PathOperatorWithTags.class, debug = DebugOption.ALL)
  public void shouldParseRouteMetadata(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: PathOperatorWithTags API\n"
        + "  description: PathOperatorWithTags API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /pets:\n"
        + "    summary: API summary\n"
        + "    description: API description\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - local\n"
        + "      - top\n"
        + "      summary: List pets\n"
        + "      description: Pets ...\n"
        + "      operationId: getPets\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n", result.toYaml());
  }

}
