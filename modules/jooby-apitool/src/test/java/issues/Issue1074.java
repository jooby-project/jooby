package issues;

import kt.App1074;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteMethodAssert;
import org.jooby.apitool.RouteParameter;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class Issue1074 extends ApiToolFeature {

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
          r.returns(null);
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
        + "      operationId: \"getByPagePage-size\"\n"
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
        + "            type: \"string\"\n", yaml(swagger(routes)));
  }

}
