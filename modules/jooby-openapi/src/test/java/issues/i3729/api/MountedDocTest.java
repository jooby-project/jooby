/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class MountedDocTest {

  @OpenAPITest(value = MountedApp.class)
  public void shouldGenerateDocFromMountedApp(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: Mounted API\n"
            + "  description: Mounted API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /main:\n"
            + "    get:\n"
            + "      summary: This is the main router.\n"
            + "      operationId: getMain\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "  /mounted:\n"
            + "    get:\n"
            + "      summary: Mounted operation.\n"
            + "      operationId: mountedOp3\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "  /mount-point/mounted:\n"
            + "    get:\n"
            + "      summary: Mounted operation.\n"
            + "      operationId: mountedOp2\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "  /installed:\n"
            + "    get:\n"
            + "      summary: Installed operation.\n"
            + "      operationId: installedOp\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "  /install-point/installed:\n"
            + "    get:\n"
            + "      summary: Installed operation.\n"
            + "      operationId: installedOp2\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "  /install-point/mounted:\n"
            + "    get:\n"
            + "      summary: Mounted operation.\n"
            + "      operationId: mountedOp4\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n",
        result.toYaml());
  }
}
