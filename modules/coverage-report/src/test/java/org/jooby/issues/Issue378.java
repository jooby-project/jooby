package org.jooby.issues;

import org.jooby.MediaType;
import org.jooby.issues.i378.Cat;
import org.jooby.raml.Raml;
import org.jooby.swagger.SwaggerUI;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue378 extends ServerFeature {

  {
    /**
     * Produces Cat object
     *
     * Next line
     */
    use("/api/cat/")
        /**
         * @param name Cat's name
         *
         * @return Returns a cat {@link Cat}
         */
        .get("/:name", req -> {
          Cat cat = new Cat();
          cat.setName(req.param("name").value());

          return cat;
        })
        .produces(MediaType.json);

    new Raml().install(this);
    new SwaggerUI().install(this);
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
