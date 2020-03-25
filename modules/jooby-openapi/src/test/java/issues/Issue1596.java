package issues;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import issues.i1596.PathOperatorWithTags;
import issues.i1596.ClassLevelTagApp;
import kt.KtPathOperatorWithTags;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1596 {

  @OpenAPITest(value = PathOperatorWithTags.class)
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
        + "      - super\n"
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

  @OpenAPITest(value = ClassLevelTagApp.class)
  public void shouldParseClassLevelTags(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: ClassLevelTag API\n"
        + "  description: ClassLevelTag API description\n"
        + "  version: \"1.0\"\n"
        + "tags:\n"
        + "- name: pets\n"
        + "  description: Group pets\n"
        + "- name: query\n"
        + "  description: Search API\n"
        + "paths:\n"
        + "  /:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - pets\n"
        + "      - query\n"
        + "      summary: Get Pets\n"
        + "      operationId: getPets\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: array\n"
        + "                items:\n"
        + "                  type: string\n"
        + "    post:\n"
        + "      tags:\n"
        + "      - pets\n"
        + "      summary: Create Pets\n"
        + "      operationId: createPets\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n", result.toYaml());
  }

  @OpenAPITest(value = KtPathOperatorWithTags.class)
  public void shouldParseRouteMetadataKt(OpenAPIResult result) {
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
        + "      - super\n"
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
