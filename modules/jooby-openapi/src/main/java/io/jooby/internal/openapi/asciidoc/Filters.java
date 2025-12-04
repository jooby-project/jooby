/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public enum Filters implements Filter {
  json {
    @Override
    public List<String> getArgumentNames() {
      return null;
    }

    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      try {
        var json = InternalContext.json(context);
        return "[source,json]\n----\n"
            + json.writer().withDefaultPrettyPrinter().writeValueAsString(input)
            + "\n----";
      } catch (JsonProcessingException e) {
        throw new PebbleException(
            e, "Could not convert to JSON: " + input, lineNumber, self.getName());
      }
    }
  },

  yaml {
    @Override
    public List<String> getArgumentNames() {
      return null;
    }

    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      try {
        var yaml = InternalContext.yaml(context);
        return "[source,yaml]\n----\n"
            + yaml.writer().withDefaultPrettyPrinter().writeValueAsString(input)
            + "----";
      } catch (JsonProcessingException e) {
        throw new PebbleException(
            e, "Could not convert to YAML: " + input, lineNumber, self.getName());
      }
    }
  };

  public static Map<String, Filter> allFilters() {
    Map<String, Filter> functions = new HashMap<>();
    for (var value : values()) {
      functions.put(value.name(), value);
    }
    for (var value : OperationFilters.values()) {
      functions.put(value.name(), value);
    }
    return functions;
  }
}
