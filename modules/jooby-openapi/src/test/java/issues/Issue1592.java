package issues;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import issues.i1592.App1592;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1592 {

  @OpenAPITest(value = App1592.class)
  public void shouldParseNestedTypes(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 1592 API\n"
        + "  description: 1592 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /nested:\n"
        + "    post:\n"
        + "      operationId: postNested\n"
        + "      requestBody:\n"
        + "        content:\n"
        + "          application/json:\n"
        + "            schema:\n"
        + "              $ref: '#/components/schemas/FairData'\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                $ref: '#/components/schemas/FairData'\n"
        + "components:\n"
        + "  schemas:\n"
        + "    FairEmissionData:\n"
        + "      type: object\n"
        + "      properties:\n"
        + "        co2Emissions:\n"
        + "          type: array\n"
        + "          items:\n"
        + "            type: number\n"
        + "            format: double\n"
        + "    FairData:\n"
        + "      type: object\n"
        + "      properties:\n"
        + "        baseYear:\n"
        + "          type: integer\n"
        + "          format: int32\n"
        + "        finalYear:\n"
        + "          type: integer\n"
        + "          format: int32\n"
        + "        annualEmissions:\n"
        + "          $ref: '#/components/schemas/FairEmissionData'\n", result.toYaml());
  }

}
