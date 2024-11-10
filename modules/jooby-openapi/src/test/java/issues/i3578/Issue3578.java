/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3578;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.internal.openapi.OpenApiTemplate;
import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3578 {

  @OpenAPITest(value = App3578.class)
  public void shouldMergeFromTemplate(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: Pets API\n"
            + "  description: Nunc fermentum ipsum id bibendum blandit. Praesent sagittis est ut.\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /api/pets:\n"
            + "    get:\n"
            + "      operationId: listPets\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "    post:\n"
            + "      operationId: createPet\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "  /api/pets/{id}:\n"
            + "    get:\n"
            + "      operationId: findPetById\n"
            + "      parameters:\n"
            + "      - name: id\n"
            + "        in: path\n"
            + "        required: true\n"
            + "        schema:\n"
            + "          type: string\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "    put:\n"
            + "      description: Update a valid pet\n"
            + "      operationId: updatePet\n"
            + "      parameters:\n"
            + "      - name: id\n"
            + "        in: path\n"
            + "        required: true\n"
            + "        schema:\n"
            + "          type: string\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "    delete:\n"
            + "      description: Delete a Pet from database\n"
            + "      operationId: deletePet\n"
            + "      parameters:\n"
            + "      - name: id\n"
            + "        in: path\n"
            + "        description: Pet ID to delete\n"
            + "        required: true\n"
            + "        schema:\n"
            + "          type: string\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n",
        result.toYaml());
  }

  @OpenAPITest(value = MergePaths.class, templateName = "issues/i3578/fail-unknown.yaml")
  public void shouldFailOnUnknownPath(OpenAPIResult result) {
    var message = assertThrows(IllegalArgumentException.class, result::toYaml).getMessage();
    assertEquals("Unknown path: \"/app-path/pets\"", message);
  }

  @OpenAPITest(value = MergePaths.class, templateName = "issues/i3578/keep.yaml")
  public void shouldKeepFromTemplate(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: Pets API\n"
            + "  description: Nunc fermentum ipsum id bibendum blandit. Praesent sagittis est ut.\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /app-path:\n"
            + "    get:\n"
            + "      operationId: home\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n"
            + "  /app-path/pets:\n"
            + "    get:\n"
            + "      operationId: listPets\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n",
        result.toYaml());
  }

  @Test
  public void parseParamWithoutIn() throws IOException {
    var out =
        OpenApiTemplate.yaml.readValue(
            Paths.get("src", "test", "resources", "issues", "i3578", "app3578.yaml").toFile(),
            OpenAPIExt.class);
    var op = out.getPaths().get("/api/pets/{id}").getDelete();
    assertNotNull(op);
    assertNotNull(op.getParameters());
    // must be a partial parameter
    assertEquals(1, op.getParameters().size());
  }
}
