package org.jooby.apitool;

import apps.AppWithDoc;
import org.jooby.apitool.raml.Raml;
import org.jooby.apitool.raml.RamlType;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RamlTest {

  public class Foo {

  }

  public class Client {
    private UUID id;
    private String name;

    public UUID getId() {
      return id;
    }

    public void setId(final UUID id) {
      this.id = id;
    }
  }

  @Test
  public void shouldWorkWithUUID() throws Exception {
    RouteMethod method = new RouteMethod();
    method.pattern("/api/entry/{id}")
        .method("GET")
        .parameters(
            Arrays.asList(new RouteParameter("id", RouteParameter.Kind.PATH, UUID.class, null)))
        .response(new RouteResponse().type(Client.class));

    Raml base = new Raml();
    RamlType uuid = base.define(UUID.class, RamlType.STRING);
    uuid.setPattern(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    List<RouteMethod> methods = Arrays.asList(method);
    Raml raml = Raml.build(base, methods);

    String yaml = raml.toYaml();
    assertEquals("#%RAML 1.0\n"
        + "---\n"
        + "mediaType:\n"
        + "- application/json\n"
        + "types:\n"
        + "  UUID:\n"
        + "    type: string\n"
        + "    pattern: ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$\n"
        + "  Client:\n"
        + "    type: object\n"
        + "    properties:\n"
        + "      id?: UUID\n"
        + "/api:\n"
        + "  /entry:\n"
        + "    /{id}:\n"
        + "      uriParameters:\n"
        + "        id:\n"
        + "          required: true\n"
        + "          type: UUID\n"
        + "      get:\n"
        + "        responses:\n"
        + "          200:\n"
        + "            description: Client\n"
        + "            body:\n"
        + "              application/json:\n"
        + "                type: Client\n", yaml);
  }

  @Test
  public void shouldUseCustomType() throws Exception {
    RouteMethod method = new RouteMethod();
    method.pattern("/api/entry/{id}")
        .method("GET")
        .parameters(
            Arrays.asList(new RouteParameter("id", RouteParameter.Kind.PATH, UUID.class, null)))
        .response(new RouteResponse().type(Foo.class));

    Raml base = new Raml();
    RamlType customObject = base.define(Foo.class, new RamlType("object"));
    customObject.newProperty("bar", "string", true);
    List<RouteMethod> methods = Arrays.asList(method);
    Raml raml = Raml.build(base, methods);

    String yaml = raml.toYaml();
    assertEquals("#%RAML 1.0\n"
        + "---\n"
        + "mediaType:\n"
        + "- application/json\n"
        + "types:\n"
        + "  Foo:\n"
        + "    type: object\n"
        + "    properties:\n"
        + "      bar: string\n"
        + "  UUID:\n"
        + "    type: object\n"
        + "/api:\n"
        + "  /entry:\n"
        + "    /{id}:\n"
        + "      uriParameters:\n"
        + "        id:\n"
        + "          required: true\n"
        + "          type: UUID\n"
        + "      get:\n"
        + "        responses:\n"
        + "          200:\n"
        + "            description: Foo\n"
        + "            body:\n"
        + "              application/json:\n"
        + "                type: Foo\n", yaml);
  }

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
        + "        description: String\n"
        + "        body:\n"
        + "          application/json:\n"
        + "            type: string\n"
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
        + "            application/json:\n"
        + "              type: Pet[]\n"
        + "    post:\n"
        + "      description: Add a new pet to the store.\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: Returns a saved pet.\n"
        + "          body:\n"
        + "            application/json:\n"
        + "              type: Pet\n"
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
        + "              application/json:\n"
        + "                type: Pet\n"
        + "          404:\n"
        + "            description: Not Found\n"
        + "      delete:\n"
        + "        description: Deletes a pet by ID.\n"
        + "        responses:\n"
        + "          204:\n"
        + "            description: A <code>204</code>\n"
        + "            body:\n"
        + "              application/json:\n"
        + "                type: Pet\n", yaml);
  }

  private Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
