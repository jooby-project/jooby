package issues;

import apps.App1182;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class Issue1182 extends ApiToolFeature {

  @Test
  public void shouldProcessApiParamSwaggerAnnotation() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1182());

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"1182\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /api/1182:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"1182\"\n"
        + "      summary: \"Api param works\"\n"
        + "      operationId: \"/Controller1182.list\"\n"
        + "      parameters:\n"
        + "      - name: \"foo\"\n"
        + "        in: \"query\"\n"
        + "        description: \"bar name is ignored\"\n"
        + "        required: true\n"
        + "        type: \"string\"\n"
        + "      - name: \"q\"\n"
        + "        in: \"query\"\n"
        + "        description: \"Search query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"String\"\n", yaml(swagger(routes), false));
  }

}
