package issues;

import io.swagger.util.Yaml;
import kt.App1072;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteMethodAssert;
import org.jooby.internal.apitool.SwaggerBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Issue1072 {

  @Test
  public void shouldContainsSwaggerResponseDescription() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1072());
    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType("kt.Person");
          r.method("GET");
          r.pattern("/");
          r.description(null);
          r.summary(null);
          r.returns("Person");
        })
        .done();
    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"/\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"/\"\n"
        + "      operationId: \"get/\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"Person\"\n"
        + "          schema:\n"
        + "            $ref: \"#/definitions/Person\"\n"
        + "definitions:\n"
        + "  Person:\n"
        + "    type: \"object\"\n"
        + "    required:\n"
        + "    - \"name\"\n"
        + "    properties:\n"
        + "      name:\n"
        + "        type: \"string\"\n"
        + "      firstname:\n"
        + "        type: \"string\"\n", Yaml
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
