package issues;

import io.swagger.util.Yaml;
import kt.App1074;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteMethodAssert;
import org.jooby.apitool.RouteParameter;
import org.jooby.internal.apitool.SwaggerBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Issue1074 {

  @Test
  public void shouldDetectKotlinTypeUsingOptionalParameters() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1074());

    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType(String.class);
          r.method("GET");
          r.pattern("/");
          r.description(null);
          r.summary(null);
          r.returns("String");
          r.param(p -> {
            p.type("java.util.Optional<java.lang.Integer>");
            p.name("page");
            p.kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.type("java.util.Optional<java.lang.Integer>");
            p.name("page-size");
            p.kind(RouteParameter.Kind.QUERY);
          });
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
        + "      parameters:\n"
        + "      - name: \"page\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"integer\"\n"
        + "        format: \"int32\"\n"
        + "      - name: \"page-size\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"integer\"\n"
        + "        format: \"int32\"\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n", Yaml
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
