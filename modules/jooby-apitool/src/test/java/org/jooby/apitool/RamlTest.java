package org.jooby.apitool;

import apps.AppWithDoc;
import org.jooby.apitool.raml.Raml;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RamlTest {
  @Test
  public void shouldExportToRaml() throws Exception {
    ApiParser parser = new ApiParser(dir());

    List<RouteMethod> methods = parser.parseFully(new AppWithDoc());

    String yaml = Raml.build(null, methods)
        .toYaml();
    assertEquals("#%RAML 1.0\n"
        + "---\n"
        + "mediaType:\n"
        + "- application/json\n"
        + "types:\n"
        + "  Category:\n"
        + "    type: object\n"
        + "    properties:\n"
        + "      id?: integer\n"
        + "      name?: string\n"
        + "    example:\n"
        + "      id: 0\n"
        + "      name: string\n"
        + "  Pet:\n"
        + "    type: object\n"
        + "    properties:\n"
        + "      id?: integer\n"
        + "      name?: string\n"
        + "      category?: Category\n"
        + "      photoUrls?: string[]\n"
        + "      tags?: Tag[]\n"
        + "      status?:\n"
        + "        enum:\n"
        + "        - available\n"
        + "        - not_available\n"
        + "    example:\n"
        + "      id: 0\n"
        + "      name: string\n"
        + "      status: available\n"
        + "  Tag:\n"
        + "    type: object\n"
        + "    properties:\n"
        + "      id?: integer\n"
        + "      name?: string\n"
        + "    example:\n"
        + "      id: 0\n"
        + "      name: string\n"
        + "/:\n"
        + "  get:\n"
        + "    description: Home page.\n"
        + "    responses:\n"
        + "      200:\n"
        + "        body:\n"
        + "          type: string\n"
        + "/api:\n"
        + "  /pets:\n"
        + "    description: Everything about your Pets.\n"
        + "    get:\n"
        + "      description: List pets ordered by id.\n"
        + "      queryParameters:\n"
        + "        start:\n"
        + "          required: false\n"
        + "          description: Start offset, useful for paging. Default is <code>0</code>.\n"
        + "          default: 0\n"
        + "          type: integer\n"
        + "        max:\n"
        + "          required: false\n"
        + "          description: Max page size, useful for paging. Default is <code>50</code>.\n"
        + "          default: 200\n"
        + "          type: integer\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: Pets ordered by name.\n"
        + "          body:\n"
        + "            type: Pet[]\n"
        + "    post:\n"
        + "      description: Add a new pet to the store.\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: Returns a saved pet.\n"
        + "          body:\n"
        + "            type: Pet\n"
        + "      body:\n"
        + "        type: Pet\n"
        + "    /{id}:\n"
        + "      description: Everything about your Pets.\n"
        + "      uriParameters:\n"
        + "        id:\n"
        + "          required: true\n"
        + "          description: Pet ID.\n"
        + "          type: integer\n"
        + "      get:\n"
        + "        description: Find pet by ID.\n"
        + "        responses:\n"
        + "          200:\n"
        + "            description: Returns <code>200</code> with a single pet or <code>404</code>\n"
        + "            body:\n"
        + "              type: Pet\n"
        + "          404:\n"
        + "            description: Not Found\n"
        + "      delete:\n"
        + "        description: Deletes a pet by ID.\n"
        + "        responses:\n"
        + "          204:\n"
        + "            description: A <code>204</code>\n"
        + "            body:\n"
        + "              type: Pet\n", yaml);
  }

  private Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
