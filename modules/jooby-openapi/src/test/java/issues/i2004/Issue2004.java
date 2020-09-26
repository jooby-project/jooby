package issues.i2004;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import kt.issues.i2004.App2004;

public class Issue2004 {
  @OpenAPITest(value = App2004.class)
  public void shouldGenerateDocForParameter(OpenAPIResult result) {
    assertEquals("openapi: 3.0.1\n"
        + "info:\n"
        + "  title: 2004 API\n"
        + "  description: 2004 API description\n"
        + "  version: \"1.0\"\n"
        + "paths:\n"
        + "  /issues/2004:\n"
        + "    get:\n"
        + "      summary: Get all companies\n"
        + "      description: Find all companies defined by the search parameters\n"
        + "      operationId: getAllCompanies\n"
        + "      requestBody:\n"
        + "        content:\n"
        + "          application/json:\n"
        + "            schema:\n"
        + "              $ref: '#/components/schemas/CompanyRequest'\n"
        + "        required: true\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: Success\n"
        + "          content:\n"
        + "            application/json:\n"
        + "              schema:\n"
        + "                type: string\n"
        + "components:\n"
        + "  schemas:\n"
        + "    CompanyRequest:\n"
        + "      type: object\n"
        + "      properties:\n"
        + "        baseYear:\n"
        + "          type: integer\n"
        + "          format: int32\n"
        + "        naceCode:\n"
        + "          type: array\n"
        + "          items:\n"
        + "            type: string\n"
        + "        region:\n"
        + "          type: string\n"
        + "          description: blablabla\n"
        + "          example: HEY\n"
        + "      description: This is a company request\n", result.toYaml());
  }
}
