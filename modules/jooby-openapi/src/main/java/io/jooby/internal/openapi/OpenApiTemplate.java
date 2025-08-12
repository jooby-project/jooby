/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jooby.SneakyThrows;
import io.swagger.v3.core.util.*;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.parameters.Parameter;

public class OpenApiTemplate {

  private static class PartialParameterDeserializer extends ParameterDeserializer {
    @Override
    public Parameter deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
      var codec = jp.getCodec();
      ObjectNode node = codec.readTree(jp);
      var factory = codec.getFactory();
      var result = parse(factory, ctx, node.toString());
      if (result == null) {
        // fake a query parameter
        node.put("in", "query");
        // now convert
        result = parse(factory, ctx, node.toString());
        // reset in
        result.setIn(null);
      }
      return result;
    }

    private Parameter parse(JsonFactory factory, DeserializationContext ctx, String json)
        throws IOException {
      try (var jp1 = factory.createParser(json)) {
        return super.deserialize(jp1, ctx);
      }
    }
  }

  private final ObjectMapper json;
  private final ObjectMapper yaml;
  private final SpecVersion version;

  public OpenApiTemplate(SpecVersion version) {
    this.json = override(version == SpecVersion.V30 ? Json.mapper() : Json31.mapper());
    this.yaml = override(version == SpecVersion.V30 ? Yaml.mapper() : Yaml31.mapper());
    this.version = version;
  }

  public ObjectMapper json() {
    return json;
  }

  public ObjectMapper yaml() {
    return yaml;
  }

  private static ObjectMapper override(ObjectMapper mapper) {
    var partial = new PartialParameterDeserializer();
    var deserializers = new SimpleDeserializers();
    deserializers.addDeserializer(Parameter.class, partial);

    var override = new SimpleModule();
    override.setDeserializers(deserializers);
    mapper.registerModule(override);
    return mapper;
  }

  public OpenAPIExt fromTemplate(Path basedir, ClassLoader classLoader, String templateName) {
    var openapi = createTemplate(basedir, classLoader, templateName);
    openapi.setSpecVersion(version);
    return openapi;
  }

  private OpenAPIExt createTemplate(Path basedir, ClassLoader classLoader, String templateName) {
    try {

      Path path = basedir.resolve("conf").resolve(templateName);
      boolean yamlExt = templateName.endsWith(".yaml") || templateName.endsWith(".yml");
      if (Files.exists(path)) {
        return yamlExt
            ? yaml.readValue(path.toFile(), OpenAPIExt.class)
            : json.readValue(path.toFile(), OpenAPIExt.class);
      }
      URL resource = classLoader.getResource(templateName);
      if (resource != null) {
        return yamlExt
            ? yaml.readValue(resource, OpenAPIExt.class)
            : json.readValue(resource, OpenAPIExt.class);
      }
      return new OpenAPIExt();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
