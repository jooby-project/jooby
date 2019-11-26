package issues;

import kt.App1075;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class Issue1075 extends ApiToolFeature {

  @Test
  public void shouldGenerateUniqueOperationIds() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1075());

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"Orders\"\n"
        + "- name: \"Products\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /v2/orders:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"Orders\"\n"
        + "      operationId: \"getOrders\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"List[String]\"\n"
        + "          schema:\n"
        + "            type: \"array\"\n"
        + "            items:\n"
        + "              type: \"string\"\n"
        + "  /v2/orders/{id}:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"Orders\"\n"
        + "      operationId: \"getOrdersById\"\n"
        + "      parameters:\n"
        + "      - name: \"id\"\n"
        + "        in: \"path\"\n"
        + "        required: true\n"
        + "        type: \"integer\"\n"
        + "        format: \"int32\"\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"int\"\n"
        + "          schema:\n"
        + "            type: \"integer\"\n"
        + "            format: \"int32\"\n"
        + "  /v2/products:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"Products\"\n"
        + "      operationId: \"getProducts\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"List[String]\"\n"
        + "          schema:\n"
        + "            type: \"array\"\n"
        + "            items:\n"
        + "              type: \"string\"\n", yaml(swagger(routes)));
  }

}
