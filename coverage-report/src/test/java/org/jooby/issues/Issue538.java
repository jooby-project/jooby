package org.jooby.issues;

import org.jooby.issues.i378.Cat;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.mvc.Produces;
import org.jooby.raml.Raml;
import org.jooby.swagger.SwaggerUI;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue538 extends ServerFeature {

  {
    use(CatResource.class);

    new Raml().install(this);
    new SwaggerUI().install(this);
  }

  /**
   * Produces Cat object
   *
   * Next line
   */
  @Path("/api/cat")
  public static class CatResource {

    /**
     * @param name Cat's name
     * @return Returns a cat {@link Cat}
     */
    @Produces("application/json")
    @Path("/:name")
    @GET
    public Cat get(final String name) {
      Cat cat = new Cat();
      cat.setName(name);

      return cat;
    }
  }

  @Test
  public void shouldGenerateAValidRaml() throws Exception {
    request()
        .get("/raml/api.raml")
        .expect("#%RAML 1.0\n" +
            "baseUri: http://localhost:" + port + "/\n"
            + "mediaType: application/json\n"
            + "protocols: [HTTP]\n"
            + "title: issues API\n"
            + "version: 0.0.0\n" +
            "types:\n" +
            "  Cat:\n" +
            "    type: object\n" +
            "    properties:\n" +
            "      name:\n" +
            "        type: string\n" +
            "/api/cat/{name}:\n" +
            "  uriParameters:\n" +
            "    name:\n" +
            "      type: string\n" +
            "      description: 'Cat''s name'\n" +
            "      required: true\n" +
            "  description: |-\n" +
            "     Produces Cat object\n" +
            "     Next line\n" +
            "  get:\n" +
            "    responses:\n" +
            "      200:\n" +
            "        description: 'Returns a cat {@link Cat}'\n" +
            "        body:\n" +
            "          application/json:\n" +
            "            type: Cat");
  }

  @Test
  public void shouldGenerateAValidSwagger() throws Exception {
    request()
        .get("/swagger/swagger.json")
        .expect("{\n" +
            "  \"swagger\" : \"2.0\",\n" +
            "  \"info\" : {\n" +
            "    \"version\" : \"0.0.0\",\n" +
            "    \"title\" : \"issues API\"\n" +
            "  },\n" +
            "  \"basePath\" : \"/\",\n" +
            "  \"tags\" : [ {\n" +
            "    \"name\" : \"cat\",\n" +
            "    \"description\" : \"Produces Cat object\\nNext line\"\n" +
            "  } ],\n" +
            "  \"schemes\" : [ \"http\" ],\n" +
            "  \"consumes\" : [ \"application/json\" ],\n" +
            "  \"produces\" : [ \"application/json\" ],\n" +
            "  \"paths\" : {\n" +
            "    \"/api/cat/{name}\" : {\n" +
            "      \"get\" : {\n" +
            "        \"tags\" : [ \"cat\" ],\n" +
            "        \"summary\" : \"CatResource.get\",\n" +
            "        \"produces\" : [ \"application/json\" ],\n" +
            "        \"parameters\" : [ {\n" +
            "          \"name\" : \"name\",\n" +
            "          \"in\" : \"path\",\n" +
            "          \"description\" : \"Cat's name\",\n" +
            "          \"required\" : true,\n" +
            "          \"type\" : \"string\"\n" +
            "        } ],\n" +
            "        \"responses\" : {\n" +
            "          \"200\" : {\n" +
            "            \"description\" : \"Returns a cat {@link Cat}\",\n" +
            "            \"schema\" : {\n" +
            "              \"$ref\" : \"#/definitions/Cat\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"definitions\" : {\n" +
            "    \"Cat\" : {\n" +
            "      \"type\" : \"object\",\n" +
            "      \"properties\" : {\n" +
            "        \"name\" : {\n" +
            "          \"type\" : \"string\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}");
  }
}
