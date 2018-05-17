package issues;

import com.google.common.collect.ImmutableList;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteResponse;
import org.jooby.apitool.raml.Raml;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Issue1050 {

  @Test
  public void mediaTypeOnControllers() throws IOException {
    RouteMethod home = new RouteMethod("get", "/", new RouteResponse(String.class));

    RouteMethod controller = new RouteMethod("get", "/controller/controller", new RouteResponse(String.class));
    controller.consumes(ImmutableList.of("application/x-www-form-urlencoded"));
    controller.produces(ImmutableList.of("application/json"));

    List<RouteMethod> methods = Arrays.asList(home, controller);
    String yaml = Raml.build(null, methods)
        .toYaml();
    assertEquals("#%RAML 1.0\n"
        + "---\n"
        + "types: {}\n"
        + "/:\n"
        + "  get:\n"
        + "    responses:\n"
        + "      200:\n"
        + "        description: java.lang.String\n"
        + "        body:\n"
        + "          application/json:\n"
        + "            type: string\n"
        + "    body:\n"
        + "      application/json: {}\n"
        + "/controller:\n"
        + "  /controller:\n"
        + "    get:\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: java.lang.String\n"
        + "          body:\n"
        + "            application/json:\n"
        + "              type: string\n"
        + "      body:\n"
        + "        application/x-www-form-urlencoded: {}\n", yaml);
  }
}
