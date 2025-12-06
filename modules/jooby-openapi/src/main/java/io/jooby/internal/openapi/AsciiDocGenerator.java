/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;

import io.jooby.internal.openapi.asciidoc.Filters;
import io.jooby.internal.openapi.asciidoc.Functions;
import io.jooby.internal.openapi.asciidoc.SnippetResolver;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.extension.*;
import io.pebbletemplates.pebble.lexer.Syntax;
import io.pebbletemplates.pebble.operator.BinaryOperator;
import io.pebbletemplates.pebble.operator.UnaryOperator;
import io.pebbletemplates.pebble.tokenParser.TokenParser;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.servers.Server;

public class AsciiDocGenerator {

  public static String generate(OpenAPIExt openAPI, Path index) throws IOException {
    var engine = newEngine(openAPI, index.getParent());
    var template = engine.getTemplate(index.toAbsolutePath().toString());
    var writer = new StringWriter();
    var context = new HashMap<String, Object>();
    template.evaluate(writer, context);
    return writer.toString().trim();
  }

  public static void export(Path baseDir, Path input, Path outputDir) {
    try (var asciidoctor = Asciidoctor.Factory.create()) {

      var options =
          Options.builder()
              .backend("html5")
              .baseDir(baseDir.toFile())
              .toDir(outputDir.toFile())
              .mkDirs(true)
              .safe(SafeMode.UNSAFE)
              .build();

      // Perform the conversion
      asciidoctor.convertFile(input.toFile(), options);
    }
  }

  @SuppressWarnings("unchecked")
  private static PebbleEngine newEngine(OpenAPIExt openapi, Path baseDir) {
    var json = (openapi.getSpecVersion() == SpecVersion.V30 ? Json.mapper() : Json31.mapper());
    var yaml = (openapi.getSpecVersion() == SpecVersion.V30 ? Yaml.mapper() : Yaml31.mapper());
    var snippetResolver = new SnippetResolver(baseDir.resolve("snippet"));
    var serverUrl =
        Optional.ofNullable(openapi.getServers())
            .map(List::getFirst)
            .map(Server::getUrl)
            .orElse("");
    var openapiRoot = json.convertValue(openapi, Map.class);
    openapiRoot.put("openapi", openapi);
    openapiRoot.put(
        "internal",
        Map.of("resolver", snippetResolver, "serverUrl", serverUrl, "json", json, "yaml", yaml));
    var engine = newEngine(new OpenApiSupport(openapiRoot));
    snippetResolver.setEngine(engine);
    return engine;
  }

  private static PebbleEngine newEngine(OpenApiSupport extension) {
    // 1. Define the custom syntax using a builder
    return new PebbleEngine.Builder()
        .extension(extension)
        .autoEscaping(false)
        .syntax(
            new Syntax.Builder()
                .setPrintOpenDelimiter("${")
                .setPrintCloseDelimiter("}")
                .setEnableNewLineTrimming(false)
                .build())
        .build();
  }

  private static class OpenApiSupport implements Extension {
    private final Map<String, Object> vars;

    public OpenApiSupport(Map<String, Object> vars) {
      this.vars = vars;
    }

    @Override
    public Map<String, Filter> getFilters() {
      return Filters.allFilters();
    }

    @Override
    public Map<String, Test> getTests() {
      return Map.of();
    }

    @Override
    public Map<String, Function> getFunctions() {
      return Functions.fn();
    }

    @Override
    public List<TokenParser> getTokenParsers() {
      return List.of();
    }

    @Override
    public List<BinaryOperator> getBinaryOperators() {
      return List.of();
    }

    @Override
    public List<UnaryOperator> getUnaryOperators() {
      return List.of();
    }

    @Override
    public Map<String, Object> getGlobalVariables() {
      return vars;
    }

    @Override
    public List<NodeVisitorFactory> getNodeVisitors() {
      return List.of();
    }

    @Override
    public List<AttributeResolver> getAttributeResolver() {
      return List.of();
    }
  }
}
