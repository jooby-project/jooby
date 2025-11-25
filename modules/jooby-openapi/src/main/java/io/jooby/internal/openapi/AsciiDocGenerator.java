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

public class AsciiDocGenerator {

  public static String generate(OpenAPIExt openAPI, Path index) throws IOException {
    var snippetResolver = new SnippetResolver(index.getParent().resolve("snippet"));
    var engine =
        newEngine(
            new OpenApiSupport(Map.of("openapi", openAPI, "snippetResolver", snippetResolver)),
            "${",
            "}");
    snippetResolver.setEngine(engine);

    var template = engine.getTemplate(index.toAbsolutePath().toString());
    var writer = new StringWriter();
    var context = new HashMap<String, Object>();
    template.evaluate(writer, context);
    return writer.toString();
  }

  private static PebbleEngine newEngine(OpenApiSupport extension, String start, String end) {
    // 1. Define the custom syntax using a builder
    return new PebbleEngine.Builder()
        .extension(extension)
        .autoEscaping(false)
        .syntax(
            new Syntax.Builder()
                .setPrintOpenDelimiter(start)
                .setPrintCloseDelimiter(end)
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
      return Filters.fn();
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
