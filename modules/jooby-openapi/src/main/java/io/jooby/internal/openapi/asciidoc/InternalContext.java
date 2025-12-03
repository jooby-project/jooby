/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.internal.openapi.OpenAPIExt;
import io.pebbletemplates.pebble.template.EvaluationContext;

public class InternalContext {

  @SuppressWarnings("unchecked")
  private static <T> T internal(EvaluationContext context, String name) {
    var internal = (Map<String, Object>) context.getVariable("internal");
    return (T) internal.get(name);
  }

  public static <T> T variable(EvaluationContext context, String name) {
    return internal(context, name);
  }

  public static OpenAPIExt openApi(EvaluationContext context) {
    return (OpenAPIExt) context.getVariable("openapi");
  }

  public static SnippetResolver resolver(EvaluationContext context) {
    return internal(context, "resolver");
  }

  public static ObjectMapper json(EvaluationContext context) {
    return internal(context, "json");
  }

  public static ObjectMapper yaml(EvaluationContext context) {
    return internal(context, "yaml");
  }
}
