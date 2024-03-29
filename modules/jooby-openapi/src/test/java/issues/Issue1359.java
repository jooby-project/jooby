/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue1359 {
  @OpenAPITest(App1359.class)
  public void shouldGenerateDefaultResponse(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 1359 API\n"
            + "  description: 1359 API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /script/1359:\n"
            + "    get:\n"
            + "      operationId: defaultResponse\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: This is the default response\n"
            + "          content:\n"
            + "            text/plain:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "        \"500\":\n"
            + "          description: Server Error\n"
            + "        \"400\":\n"
            + "          description: Bad Request\n"
            + "        \"404\":\n"
            + "          description: Not Found\n"
            + "  /controller/1359:\n"
            + "    get:\n"
            + "      operationId: defaultResponse2\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: This is the default response\n"
            + "          content:\n"
            + "            text/plain:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "        \"500\":\n"
            + "          description: Server Error\n"
            + "        \"400\":\n"
            + "          description: Bad Request\n"
            + "        \"404\":\n"
            + "          description: Not Found\n"
            + "  /controller/1359/missing:\n"
            + "    get:\n"
            + "      operationId: defaultResponseMissing\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "        \"500\":\n"
            + "          description: Server Error\n"
            + "        \"400\":\n"
            + "          description: Bad Request\n"
            + "        \"404\":\n"
            + "          description: Not Found\n"
            + "  /controller/1359/customcode:\n"
            + "    get:\n"
            + "      operationId: customStatusCode\n"
            + "      responses:\n"
            + "        \"201\":\n"
            + "          description: This is the default response\n"
            + "          content:\n"
            + "            text/plain:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "        \"500\":\n"
            + "          description: Server Error\n"
            + "        \"400\":\n"
            + "          description: Bad Request\n"
            + "        \"404\":\n"
            + "          description: Not Found\n"
            + "  /controller/1359/multiplesuccess:\n"
            + "    get:\n"
            + "      operationId: multiplesuccess\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            text/plain:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "        \"201\":\n"
            + "          description: Created\n"
            + "          content:\n"
            + "            text/plain:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "        \"500\":\n"
            + "          description: Server Error\n"
            + "        \"400\":\n"
            + "          description: Bad Request\n"
            + "        \"404\":\n"
            + "          description: Not Found\n",
        result.toYaml());
  }
}
