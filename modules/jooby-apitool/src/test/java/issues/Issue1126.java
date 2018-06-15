package issues;

import apps.App1126;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class Issue1126 extends ApiToolFeature {

  @Test
  public void bodySchemaRefVsFormData() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1126());

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"1126\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /1126:\n"
        + "    post:\n"
        + "      tags:\n"
        + "      - \"1126\"\n"
        + "      operationId: \"/Controller1126.doPing\"\n"
        + "      parameters:\n"
        + "      - in: \"body\"\n"
        + "        name: \"pingCommand\"\n"
        + "        required: true\n"
        + "        schema:\n"
        + "          $ref: \"#/definitions/PingCommand\"\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"Boolean\"\n"
        + "          schema:\n"
        + "            type: \"boolean\"\n"
        + "  /1126/form:\n"
        + "    post:\n"
        + "      tags:\n"
        + "      - \"1126\"\n"
        + "      operationId: \"/Controller1126.doForm\"\n"
        + "      consumes:\n"
        + "      - \"application/x-www-form-urlencoded\"\n"
        + "      - \"multipart/form-data\"\n"
        + "      parameters:\n"
        + "      - name: \"message\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"id\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"integer\"\n"
        + "        format: \"int32\"\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"Boolean\"\n"
        + "          schema:\n"
        + "            type: \"boolean\"\n"
        + "definitions:\n"
        + "  PingCommand:\n"
        + "    type: \"object\"\n"
        + "    properties:\n"
        + "      message:\n"
        + "        type: \"string\"\n"
        + "      id:\n"
        + "        type: \"integer\"\n"
        + "        format: \"int32\"\n", yaml(swagger(routes), false));
  }

}
