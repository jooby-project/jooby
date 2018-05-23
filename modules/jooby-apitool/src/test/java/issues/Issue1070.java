package issues;

import kt.App1070;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteMethodAssert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class Issue1070 extends ApiToolFeature {

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
        + "    \"name\" : \"Rates\",\n"
        + "    \"description\" : \"Top\\nSubTop1\"\n"
        + "  }, {\n"
        + "    \"name\" : \"Currencies\",\n"
        + "    \"description\" : \"Top\\nSubTop2\"\n"
        + "  } ],\n"
        + "  \"consumes\" : [ \"application/json\" ],\n"
        + "  \"produces\" : [ \"application/json\" ],\n"
        + "  \"paths\" : {\n"
        + "    \"/v2/currencies/rates\" : {\n"
        + "      \"get\" : {\n"
        + "        \"tags\" : [ \"Rates\" ],\n"
        + "        \"summary\" : \"yadayada\",\n"
        + "        \"description\" : \"\",\n"
        + "        \"operationId\" : \"getRates\",\n"
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
        + "        \"tags\" : [ \"Rates\" ],\n"
        + "        \"summary\" : \"yadayada2\",\n"
        + "        \"description\" : \"\",\n"
        + "        \"operationId\" : \"deleteRates\",\n"
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
        + "        \"tags\" : [ \"Currencies\" ],\n"
        + "        \"summary\" : \"Gets the currency for a given ISO code\",\n"
        + "        \"description\" : \"\",\n"
        + "        \"operationId\" : \"getCurrencies\",\n"
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
        + "}", json(swagger(routes)));

    // Now remove response text and build default response
    routes.get(0).response().description(null);
    assertEquals("{\n"
        + "  \"swagger\" : \"2.0\",\n"
        + "  \"tags\" : [ {\n"
        + "    \"name\" : \"Rates\",\n"
        + "    \"description\" : \"Top\\nSubTop1\"\n"
        + "  }, {\n"
        + "    \"name\" : \"Currencies\",\n"
        + "    \"description\" : \"Top\\nSubTop2\"\n"
        + "  } ],\n"
        + "  \"consumes\" : [ \"application/json\" ],\n"
        + "  \"produces\" : [ \"application/json\" ],\n"
        + "  \"paths\" : {\n"
        + "    \"/v2/currencies/rates\" : {\n"
        + "      \"get\" : {\n"
        + "        \"tags\" : [ \"Rates\" ],\n"
        + "        \"summary\" : \"yadayada\",\n"
        + "        \"description\" : \"\",\n"
        + "        \"operationId\" : \"getRates\",\n"
        + "        \"parameters\" : [ ],\n"
        + "        \"responses\" : {\n"
        + "          \"200\" : {\n"
        + "            \"description\" : \"java.lang.String\",\n"
        + "            \"schema\" : {\n"
        + "              \"type\" : \"string\"\n"
        + "            }\n"
        + "          }\n"
        + "        }\n"
        + "      },\n"
        + "      \"delete\" : {\n"
        + "        \"tags\" : [ \"Rates\" ],\n"
        + "        \"summary\" : \"yadayada2\",\n"
        + "        \"description\" : \"\",\n"
        + "        \"operationId\" : \"deleteRates\",\n"
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
        + "        \"tags\" : [ \"Currencies\" ],\n"
        + "        \"summary\" : \"Gets the currency for a given ISO code\",\n"
        + "        \"description\" : \"\",\n"
        + "        \"operationId\" : \"getCurrencies\",\n"
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
        + "}", json(swagger(routes)));
  }

}
