package issues;

import apps.App1276;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class Issue1276 extends ApiToolFeature {

  @Test
  public void apitoolDoesNotListMvcNamespaced () throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1276());

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"/namespace/1176\"\n"
        + "- name: \"/namespace/foo\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /namespace/1176:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"/namespace/1176\"\n"
        + "      operationId: \"/Controller1276.foo\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n"
        + "  /namespace/foo:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"/namespace/foo\"\n"
        + "      operationId: \"getNamespacefoo\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n", yaml(swagger(routes), false));
  }

}
