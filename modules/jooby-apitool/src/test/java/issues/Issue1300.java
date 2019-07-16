package issues;

import kt.App1300;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class Issue1300 extends ApiToolFeature {

  @Test
  public void apitoolDoesNotListMvcNamespaced () throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1300());

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"/route\"\n"
        + "- name: \"/subroute/hello\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /route:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"/route\"\n"
        + "      operationId: \"getRoute\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"int\"\n"
        + "          schema:\n"
        + "            type: \"integer\"\n"
        + "            format: \"int32\"\n"
        + "  /subroute/hello:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"/subroute/hello\"\n"
        + "      operationId: \"getSubroutehello\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n", yaml(swagger(routes), false));
  }

}
