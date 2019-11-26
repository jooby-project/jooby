package issues;

import kt.App1073;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class Issue1073 extends ApiToolFeature {

  @Test
  public void zonedDateMustUseDateTimeFormat() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1073());
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
        + "      operationId: \"getByTitle\"\n"
        + "      parameters:\n"
        + "      - name: \"Title\"\n"
        + "        in: \"query\"\n"
        + "        required: true\n"
        + "        type: \"string\"\n"
        + "        format: \"date-time\"\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"News\"\n"
        + "          schema:\n"
        + "            $ref: \"#/definitions/News\"\n"
        + "definitions:\n"
        + "  News:\n"
        + "    type: \"object\"\n"
        + "    required:\n"
        + "    - \"date\"\n"
        + "    - \"title\"\n"
        + "    properties:\n"
        + "      title:\n"
        + "        type: \"string\"\n"
        + "      date:\n"
        + "        type: \"string\"\n"
        + "        format: \"date-time\"\n", yaml(swagger(routes)));
  }
}
