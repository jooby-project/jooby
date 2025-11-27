/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import static io.jooby.internal.openapi.asciidoc.Filters.curl;
import static io.jooby.openapi.CurrentDir.basedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.internal.openapi.OperationExt;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.lexer.Syntax;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public class FilterTest {

  SnippetResolver resolver;

  @BeforeEach
  public void setup() {
    var openapi = new OpenAPIExt();
    resolver = new SnippetResolver(basedir("src", "test", "resources", "adoc"));
    resolver.setEngine(
        new PebbleEngine.Builder()
            .extension(
                new AbstractExtension() {
                  @Override
                  public Map<String, Object> getGlobalVariables() {
                    return Map.of("openapi", openapi);
                  }
                })
            .syntax(
                new Syntax.Builder()
                    .setPrintOpenDelimiter("${")
                    .setPrintCloseDelimiter("}")
                    .build())
            .build());
  }

  @Test
  public void curl() {
    assertEquals(
        """
        [source,bash]
        ----
        $ curl -X GET '/api/library/{isbn}'
        ----
        """,
        curl.apply(
            operation("GET", "/api/library/{isbn}"), Map.of(), template(), evaluationContext(), 1));

    // Passing arguments
    assertEquals(
        """
        [source,bash]
        ----
        $ curl -i -X GET '/api/library/{isbn}'
        ----
        """,
        curl.apply(
            operation("GET", "/api/library/{isbn}"),
            args("-i"),
            template(),
            evaluationContext(),
            1));

    // Override method
    assertEquals(
        """
        [source,bash]
        ----
        $ curl -i -X POST '/api/library/{isbn}'
        ----
        """,
        curl.apply(
            operation("GET", "/api/library/{isbn}"),
            args("-i", "-X", "POST"),
            template(),
            evaluationContext(),
            1));

    // With Accept Header
    assertEquals(
        """
        [source,bash]
        ----
        $ curl -H 'Accept: application/json' -X GET '/api/library/{isbn}'
        ----
        """,
        curl.apply(
            operation("GET", "/api/library/{isbn}", List.of(), List.of("application/json")),
            args(),
            template(),
            evaluationContext(),
            1));

    // With Override Accept Header
    assertEquals(
        """
        [source,bash]
        ----
        $ curl -H 'Accept: application/xml' -X GET '/api/library/{isbn}'
        ----
        """,
        curl.apply(
            operation("GET", "/api/library/{isbn}", List.of(), List.of("application/json")),
            args("-H", "'Accept: application/xml'"),
            template(),
            evaluationContext(),
            1));
  }

  private Map<String, Object> args(Object... args) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < args.length; i++) {
      result.put(Integer.toString(i), args[i]);
    }
    return result;
  }

  private EvaluationContext evaluationContext() {
    var context = mock(EvaluationContext.class);
    when(context.getVariable("snippetResolver")).thenReturn(resolver);
    return context;
  }

  private PebbleTemplate template() {
    var template = mock(PebbleTemplate.class);
    return template;
  }

  public OperationExt operation(String method, String pattern) {
    var operation = mock(OperationExt.class);
    when(operation.getMethod()).thenReturn(method);
    when(operation.getPattern()).thenReturn(pattern);
    return operation;
  }

  public OperationExt operation(
      String method, String pattern, List<String> consumes, List<String> produces) {
    var operation = operation(method, pattern);
    when(operation.getConsumes()).thenReturn(consumes);
    when(operation.getProduces()).thenReturn(produces);
    return operation;
  }
}
