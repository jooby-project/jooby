/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.*;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

/**
 * GET("path") | table GET("path") | parameters | table
 *
 * <p>schema("Book") | json schema("Book.type") | yaml
 *
 * <p>GET("path") | response | json
 *
 * <p>GET("path") | response(200) | json
 *
 * <p>GET("path") | request | json
 *
 * <p>GET("path") | request | http
 *
 * <p>GET("path") | request | body | http
 */
public enum Lookup implements Function {
  operation {
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var method = args.get("method").toString();
      var path = args.get("path").toString();
      var asciidoc = AsciiDocContext.from(context);
      return asciidoc.getOpenApi().findOperation(method, path);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("method", "path");
    }
  },
  GET {
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      return operation.execute(appendMethod(args), self, context, lineNumber);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("path");
    }
  },
  POST {
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      return operation.execute(appendMethod(args), self, context, lineNumber);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("path");
    }
  },
  PUT {
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      return operation.execute(appendMethod(args), self, context, lineNumber);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("path");
    }
  },
  PATCH {
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      return operation.execute(appendMethod(args), self, context, lineNumber);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("path");
    }
  },
  DELETE {
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      return operation.execute(appendMethod(args), self, context, lineNumber);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("path");
    }
  },
  schema {
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var path = args.get("path").toString();
      var asciidoc = AsciiDocContext.from(context);
      return asciidoc.resolveSchema(path);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("path");
    }
  },
  model {
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      return schema.execute(args, self, context, lineNumber);
    }

    @Override
    public List<String> getArgumentNames() {
      return schema.getArgumentNames();
    }
  },
  tag {
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var asciidoc = AsciiDocContext.from(context);
      var name = args.get("name").toString();
      return asciidoc.getOpenApi().getTags().stream()
          .filter(tag -> tag.getName().equalsIgnoreCase(name))
          .findFirst()
          .orElseThrow(() -> new NoSuchElementException("Tag not found: " + name));
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("name");
    }
  };

  protected Map<String, Object> appendMethod(Map<String, Object> args) {
    Map<String, Object> result = new LinkedHashMap<>(args);
    result.put("method", name());
    return result;
  }

  public static Map<String, Function> lookup() {
    Map<String, Function> result = new HashMap<>();
    for (var value : values()) {
      result.put(value.name(), value);
    }
    return result;
  }
}
