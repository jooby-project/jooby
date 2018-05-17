package issues;

import io.swagger.util.Yaml;
import kt.App1075;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.RouteMethod;
import org.jooby.internal.apitool.SwaggerBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Issue1075 {

  @Test
  public void shouldGenerateUniqueOperationIds() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1075());

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"orders\"\n"
        + "- name: \"products\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /v2/orders:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"orders\"\n"
        + "      operationId: \"getOrders\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"java.util.List<java.lang.String>\"\n"
        + "          schema:\n"
        + "            type: \"array\"\n"
        + "            items:\n"
        + "              type: \"string\"\n"
        + "  /v2/products:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"products\"\n"
        + "      operationId: \"getProducts\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"java.util.List<java.lang.String>\"\n"
        + "          schema:\n"
        + "            type: \"array\"\n"
        + "            items:\n"
        + "              type: \"string\"\n", Yaml
        .mapper().writer().withDefaultPrettyPrinter().writeValueAsString(new SwaggerBuilder()
            .build(null, routes)));
  }

  private Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
