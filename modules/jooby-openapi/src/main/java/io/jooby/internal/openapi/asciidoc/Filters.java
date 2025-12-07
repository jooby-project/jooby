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
import io.jooby.internal.openapi.EnumSchema;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.swagger.v3.oas.models.media.Schema;

public enum Filters implements Filter {
  display {
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
      if (input instanceof Schema<?> schema) {
        return displaySchema(schema);
      } else {
        throw new IllegalArgumentException("Unsupported input type: " + input.getClass());
      }
    }

    private Object displaySchema(Schema<?> schema) {
      if (schema instanceof EnumSchema enumSchema) {
        var sb = new StringBuilder();
        sb.append(
            """
            [cols="1,3"]
            |===
            | Type | Description

            """);
        for (var name : enumSchema.getEnum()) {
          sb.append("\n")
              .append("| *")
              .append(name)
              .append("*\n")
              .append("| ")
              .append(enumSchema.getDescription(name))
              .append("\n");
        }
        return sb.append(" |===").toString();
      }
      return null;
    }
  },

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
