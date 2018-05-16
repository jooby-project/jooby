package issues;

import io.swagger.util.Json;
import kt.App1070;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteMethodAssert;
import org.jooby.internal.apitool.SwaggerBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Issue1070 {

  @Test
  public void shouldContainsSwaggerResponseDescription() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1070());
    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType(String.class);
          r.method("GET");
          r.pattern("/v2/currencies/rates");
          r.description("yadayada.");
          r.summary("Top\nSubTop1");
          r.returns("OK");
        })
        .next(r -> {
          r.returnType(String.class);
          r.method("DELETE");
          r.pattern("/v2/currencies/rates");
          r.description("yadayada2.");
          r.summary("Top\nSubTop1");
          r.returns("```200``` OK");
        })
        .next(r -> {
          r.returnType(String.class);
          r.method("GET");
          r.pattern("/v2/currencies/{isoCode}");
          r.description("Gets the currency for a given ISO code.");
          r.summary("Top\nSubTop2");
          r.returns("....");
        })
        .done();

    assertEquals("{\n"
            + "  \"swagger\" : \"2.0\",\n"
            + "  \"tags\" : [ {\n"
            + "    \"name\" : \"V2\",\n"
            + "    \"description\" : \"Top\\nSubTop1\"\n"
            + "  } ],\n"
            + "  \"consumes\" : [ \"application/json\" ],\n"
            + "  \"produces\" : [ \"application/json\" ],\n"
            + "  \"paths\" : {\n"
            + "    \"/v2/currencies/rates\" : {\n"
            + "      \"get\" : {\n"
            + "        \"tags\" : [ \"V2\" ],\n"
            + "        \"summary\" : \"yadayada\",\n"
            + "        \"description\" : \"\",\n"
            + "        \"operationId\" : \"getV2\",\n"
            + "        \"parameters\" : [ ],\n"
            + "        \"responses\" : {\n"
            + "          \"200\" : {\n"
            + "            \"description\" : \"OK\",\n"
            + "            \"schema\" : {\n"
            + "              \"type\" : \"string\"\n"
            + "            }\n"
            + "          }\n"
            + "        }\n"
            + "      },\n"
            + "      \"delete\" : {\n"
            + "        \"tags\" : [ \"V2\" ],\n"
            + "        \"summary\" : \"yadayada2\",\n"
            + "        \"description\" : \"\",\n"
            + "        \"operationId\" : \"deleteV2\",\n"
            + "        \"parameters\" : [ ],\n"
            + "        \"responses\" : {\n"
            + "          \"200\" : {\n"
            + "            \"description\" : \"```200``` OK\",\n"
            + "            \"schema\" : {\n"
            + "              \"type\" : \"string\"\n"
            + "            }\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    },\n"
            + "    \"/v2/currencies/{isoCode}\" : {\n"
            + "      \"get\" : {\n"
            + "        \"tags\" : [ \"V2\" ],\n"
            + "        \"summary\" : \"Gets the currency for a given ISO code\",\n"
            + "        \"description\" : \"\",\n"
            + "        \"operationId\" : \"getV2\",\n"
            + "        \"parameters\" : [ ],\n"
            + "        \"responses\" : {\n"
            + "          \"200\" : {\n"
            + "            \"description\" : \"....\",\n"
            + "            \"schema\" : {\n"
            + "              \"type\" : \"string\"\n"
            + "            }\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}",
        Json.mapper().writer().withDefaultPrettyPrinter().writeValueAsString(new SwaggerBuilder()
            .build(null, routes)));

    // Now remove response text and build default response
    routes.get(0).response().description(null);
    assertEquals("{\n"
            + "  \"swagger\" : \"2.0\",\n"
            + "  \"tags\" : [ {\n"
            + "    \"name\" : \"V2\",\n"
            + "    \"description\" : \"Top\\nSubTop1\"\n"
            + "  } ],\n"
            + "  \"consumes\" : [ \"application/json\" ],\n"
            + "  \"produces\" : [ \"application/json\" ],\n"
            + "  \"paths\" : {\n"
            + "    \"/v2/currencies/rates\" : {\n"
            + "      \"get\" : {\n"
            + "        \"tags\" : [ \"V2\" ],\n"
            + "        \"summary\" : \"yadayada\",\n"
            + "        \"description\" : \"\",\n"
            + "        \"operationId\" : \"getV2\",\n"
            + "        \"parameters\" : [ ],\n"
            + "        \"responses\" : {\n"
            + "          \"200\" : {\n"
            + "            \"description\" : \"200\",\n"
            + "            \"schema\" : {\n"
            + "              \"type\" : \"string\"\n"
            + "            }\n"
            + "          }\n"
            + "        }\n"
            + "      },\n"
            + "      \"delete\" : {\n"
            + "        \"tags\" : [ \"V2\" ],\n"
            + "        \"summary\" : \"yadayada2\",\n"
            + "        \"description\" : \"\",\n"
            + "        \"operationId\" : \"deleteV2\",\n"
            + "        \"parameters\" : [ ],\n"
            + "        \"responses\" : {\n"
            + "          \"200\" : {\n"
            + "            \"description\" : \"```200``` OK\",\n"
            + "            \"schema\" : {\n"
            + "              \"type\" : \"string\"\n"
            + "            }\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    },\n"
            + "    \"/v2/currencies/{isoCode}\" : {\n"
            + "      \"get\" : {\n"
            + "        \"tags\" : [ \"V2\" ],\n"
            + "        \"summary\" : \"Gets the currency for a given ISO code\",\n"
            + "        \"description\" : \"\",\n"
            + "        \"operationId\" : \"getV2\",\n"
            + "        \"parameters\" : [ ],\n"
            + "        \"responses\" : {\n"
            + "          \"200\" : {\n"
            + "            \"description\" : \"....\",\n"
            + "            \"schema\" : {\n"
            + "              \"type\" : \"string\"\n"
            + "            }\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}",
        Json.mapper().writer().withDefaultPrettyPrinter().writeValueAsString(new SwaggerBuilder()
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
