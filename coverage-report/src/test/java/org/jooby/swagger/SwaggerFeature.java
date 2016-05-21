package org.jooby.swagger;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SwaggerFeature extends ServerFeature {

  {
    use(Pets.class);

    new SwaggerUI().install(this);
  }

  @Test
  public void ui() throws Exception {
    request()
        .get("/swagger")
        .expect(200)
        .startsWith("<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <title>Swagger UI</title>");

    request()
        .get("/swagger/pets")
        .expect(200)
        .startsWith("<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <title>Swagger UI</title>");
  }

  @Test
  public void json() throws Exception {
    request()
        .get("/swagger/swagger.json")
        .expect("{\n" +
            "  \"swagger\" : \"2.0\",\n" +
            "  \"info\" : {\n" +
            "    \"version\" : \"0.0.0\",\n" +
            "    \"title\" : \"swagger API\"\n" +
            "  },\n" +
            "  \"basePath\" : \"/\",\n" +
            "  \"tags\" : [ {\n" +
            "    \"name\" : \"pets\"\n" +
            "  } ],\n" +
            "  \"schemes\" : [ \"http\" ],\n" +
            "  \"consumes\" : [ \"application/json\" ],\n" +
            "  \"produces\" : [ \"application/json\" ],\n" +
            "  \"paths\" : {\n" +
            "    \"/api/pets\" : {\n" +
            "      \"get\" : {\n" +
            "        \"tags\" : [ \"pets\" ],\n" +
            "        \"summary\" : \"Pets.list\",\n" +
            "        \"parameters\" : [ {\n" +
            "          \"name\" : \"size\",\n" +
            "          \"in\" : \"query\",\n" +
            "          \"required\" : false,\n" +
            "          \"type\" : \"integer\",\n" +
            "          \"format\" : \"int32\"\n" +
            "        } ],\n" +
            "        \"responses\" : {\n" +
            "          \"200\" : {\n" +
            "            \"description\" : \"Success\",\n" +
            "            \"schema\" : {\n" +
            "              \"$ref\" : \"#/definitions/IterablePet\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      \"post\" : {\n" +
            "        \"tags\" : [ \"pets\" ],\n" +
            "        \"summary\" : \"Pets.create\",\n" +
            "        \"parameters\" : [ {\n" +
            "          \"in\" : \"body\",\n" +
            "          \"name\" : \"pet\",\n" +
            "          \"required\" : true,\n" +
            "          \"schema\" : {\n" +
            "            \"$ref\" : \"#/definitions/Pet\"\n" +
            "          }\n" +
            "        } ],\n" +
            "        \"responses\" : {\n" +
            "          \"200\" : {\n" +
            "            \"description\" : \"Success\",\n" +
            "            \"schema\" : {\n" +
            "              \"$ref\" : \"#/definitions/Pet\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"/api/pets/{id}\" : {\n" +
            "      \"get\" : {\n" +
            "        \"tags\" : [ \"pets\" ],\n" +
            "        \"summary\" : \"Pets.get\",\n" +
            "        \"parameters\" : [ {\n" +
            "          \"name\" : \"id\",\n" +
            "          \"in\" : \"path\",\n" +
            "          \"required\" : true,\n" +
            "          \"type\" : \"integer\",\n" +
            "          \"format\" : \"int32\"\n" +
            "        } ],\n" +
            "        \"responses\" : {\n" +
            "          \"200\" : {\n" +
            "            \"description\" : \"Success\",\n" +
            "            \"schema\" : {\n" +
            "              \"$ref\" : \"#/definitions/Pet\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"definitions\" : {\n" +
            "    \"Pet\" : {\n" +
            "      \"type\" : \"object\",\n" +
            "      \"properties\" : {\n" +
            "        \"id\" : {\n" +
            "          \"type\" : \"integer\",\n" +
            "          \"format\" : \"int32\"\n" +
            "        },\n" +
            "        \"name\" : {\n" +
            "          \"type\" : \"string\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}");

    request()
        .get("/swagger/pets/swagger.json")
        .expect("{\n" +
            "  \"swagger\" : \"2.0\",\n" +
            "  \"info\" : {\n" +
            "    \"version\" : \"0.0.0\",\n" +
            "    \"title\" : \"swagger API\"\n" +
            "  },\n" +
            "  \"basePath\" : \"/\",\n" +
            "  \"tags\" : [ {\n" +
            "    \"name\" : \"pets\"\n" +
            "  } ],\n" +
            "  \"schemes\" : [ \"http\" ],\n" +
            "  \"consumes\" : [ \"application/json\" ],\n" +
            "  \"produces\" : [ \"application/json\" ],\n" +
            "  \"paths\" : {\n" +
            "    \"/api/pets\" : {\n" +
            "      \"get\" : {\n" +
            "        \"tags\" : [ \"pets\" ],\n" +
            "        \"summary\" : \"Pets.list\",\n" +
            "        \"parameters\" : [ {\n" +
            "          \"name\" : \"size\",\n" +
            "          \"in\" : \"query\",\n" +
            "          \"required\" : false,\n" +
            "          \"type\" : \"integer\",\n" +
            "          \"format\" : \"int32\"\n" +
            "        } ],\n" +
            "        \"responses\" : {\n" +
            "          \"200\" : {\n" +
            "            \"description\" : \"Success\",\n" +
            "            \"schema\" : {\n" +
            "              \"$ref\" : \"#/definitions/IterablePet\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      \"post\" : {\n" +
            "        \"tags\" : [ \"pets\" ],\n" +
            "        \"summary\" : \"Pets.create\",\n" +
            "        \"parameters\" : [ {\n" +
            "          \"in\" : \"body\",\n" +
            "          \"name\" : \"pet\",\n" +
            "          \"required\" : true,\n" +
            "          \"schema\" : {\n" +
            "            \"$ref\" : \"#/definitions/Pet\"\n" +
            "          }\n" +
            "        } ],\n" +
            "        \"responses\" : {\n" +
            "          \"200\" : {\n" +
            "            \"description\" : \"Success\",\n" +
            "            \"schema\" : {\n" +
            "              \"$ref\" : \"#/definitions/Pet\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"/api/pets/{id}\" : {\n" +
            "      \"get\" : {\n" +
            "        \"tags\" : [ \"pets\" ],\n" +
            "        \"summary\" : \"Pets.get\",\n" +
            "        \"parameters\" : [ {\n" +
            "          \"name\" : \"id\",\n" +
            "          \"in\" : \"path\",\n" +
            "          \"required\" : true,\n" +
            "          \"type\" : \"integer\",\n" +
            "          \"format\" : \"int32\"\n" +
            "        } ],\n" +
            "        \"responses\" : {\n" +
            "          \"200\" : {\n" +
            "            \"description\" : \"Success\",\n" +
            "            \"schema\" : {\n" +
            "              \"$ref\" : \"#/definitions/Pet\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"definitions\" : {\n" +
            "    \"Pet\" : {\n" +
            "      \"type\" : \"object\",\n" +
            "      \"properties\" : {\n" +
            "        \"id\" : {\n" +
            "          \"type\" : \"integer\",\n" +
            "          \"format\" : \"int32\"\n" +
            "        },\n" +
            "        \"name\" : {\n" +
            "          \"type\" : \"string\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}");
  }

  @Test
  public void yml() throws Exception {
    request()
        .get("/swagger/swagger.yml")
        .expect("---\n" +
            "swagger: \"2.0\"\n" +
            "info:\n" +
            "  version: \"0.0.0\"\n" +
            "  title: \"swagger API\"\n" +
            "basePath: \"/\"\n" +
            "tags:\n" +
            "- name: \"pets\"\n" +
            "schemes:\n" +
            "- \"http\"\n" +
            "consumes:\n" +
            "- \"application/json\"\n" +
            "produces:\n" +
            "- \"application/json\"\n" +
            "paths:\n" +
            "  /api/pets:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - \"pets\"\n" +
            "      summary: \"Pets.list\"\n" +
            "      parameters:\n" +
            "      - name: \"size\"\n" +
            "        in: \"query\"\n" +
            "        required: false\n" +
            "        type: \"integer\"\n" +
            "        format: \"int32\"\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: \"Success\"\n" +
            "          schema:\n" +
            "            $ref: \"#/definitions/IterablePet\"\n" +
            "    post:\n" +
            "      tags:\n" +
            "      - \"pets\"\n" +
            "      summary: \"Pets.create\"\n" +
            "      parameters:\n" +
            "      - in: \"body\"\n" +
            "        name: \"pet\"\n" +
            "        required: true\n" +
            "        schema:\n" +
            "          $ref: \"#/definitions/Pet\"\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: \"Success\"\n" +
            "          schema:\n" +
            "            $ref: \"#/definitions/Pet\"\n" +
            "  /api/pets/{id}:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - \"pets\"\n" +
            "      summary: \"Pets.get\"\n" +
            "      parameters:\n" +
            "      - name: \"id\"\n" +
            "        in: \"path\"\n" +
            "        required: true\n" +
            "        type: \"integer\"\n" +
            "        format: \"int32\"\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: \"Success\"\n" +
            "          schema:\n" +
            "            $ref: \"#/definitions/Pet\"\n" +
            "definitions:\n" +
            "  Pet:\n" +
            "    type: \"object\"\n" +
            "    properties:\n" +
            "      id:\n" +
            "        type: \"integer\"\n" +
            "        format: \"int32\"\n" +
            "      name:\n" +
            "        type: \"string\"\n" +
            "");

    request()
        .get("/swagger/pets/swagger.yml")
        .expect("---\n" +
            "swagger: \"2.0\"\n" +
            "info:\n" +
            "  version: \"0.0.0\"\n" +
            "  title: \"swagger API\"\n" +
            "basePath: \"/\"\n" +
            "tags:\n" +
            "- name: \"pets\"\n" +
            "schemes:\n" +
            "- \"http\"\n" +
            "consumes:\n" +
            "- \"application/json\"\n" +
            "produces:\n" +
            "- \"application/json\"\n" +
            "paths:\n" +
            "  /api/pets:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - \"pets\"\n" +
            "      summary: \"Pets.list\"\n" +
            "      parameters:\n" +
            "      - name: \"size\"\n" +
            "        in: \"query\"\n" +
            "        required: false\n" +
            "        type: \"integer\"\n" +
            "        format: \"int32\"\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: \"Success\"\n" +
            "          schema:\n" +
            "            $ref: \"#/definitions/IterablePet\"\n" +
            "    post:\n" +
            "      tags:\n" +
            "      - \"pets\"\n" +
            "      summary: \"Pets.create\"\n" +
            "      parameters:\n" +
            "      - in: \"body\"\n" +
            "        name: \"pet\"\n" +
            "        required: true\n" +
            "        schema:\n" +
            "          $ref: \"#/definitions/Pet\"\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: \"Success\"\n" +
            "          schema:\n" +
            "            $ref: \"#/definitions/Pet\"\n" +
            "  /api/pets/{id}:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - \"pets\"\n" +
            "      summary: \"Pets.get\"\n" +
            "      parameters:\n" +
            "      - name: \"id\"\n" +
            "        in: \"path\"\n" +
            "        required: true\n" +
            "        type: \"integer\"\n" +
            "        format: \"int32\"\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: \"Success\"\n" +
            "          schema:\n" +
            "            $ref: \"#/definitions/Pet\"\n" +
            "definitions:\n" +
            "  Pet:\n" +
            "    type: \"object\"\n" +
            "    properties:\n" +
            "      id:\n" +
            "        type: \"integer\"\n" +
            "        format: \"int32\"\n" +
            "      name:\n" +
            "        type: \"string\"\n" +
            "");
  }

}
