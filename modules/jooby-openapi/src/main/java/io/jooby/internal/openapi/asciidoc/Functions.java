/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.swagger.v3.oas.models.media.Schema;

public enum Functions implements Function {
  GET {
    @Override
    public List<String> getArgumentNames() {
      return List.of("pattern");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      args.put("identifier", name());
      return operation.execute(args, self, context, lineNumber);
    }
  },
  POST {
    @Override
    public List<String> getArgumentNames() {
      return List.of("pattern");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      args.put("identifier", name());
      return operation.execute(args, self, context, lineNumber);
    }
  },
  PUT {
    @Override
    public List<String> getArgumentNames() {
      return List.of("pattern");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      args.put("identifier", name());
      return operation.execute(args, self, context, lineNumber);
    }
  },
  PATCH {
    @Override
    public List<String> getArgumentNames() {
      return List.of("pattern");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      args.put("identifier", name());
      return operation.execute(args, self, context, lineNumber);
    }
  },
  DELETE {
    @Override
    public List<String> getArgumentNames() {
      return List.of("pattern");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      args.put("identifier", name());
      return operation.execute(args, self, context, lineNumber);
    }
  },
  tag {
    @Override
    public List<String> getArgumentNames() {
      return List.of("name");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var openApi = InternalContext.openApi(context);
      var name = args.get("name");
      return openApi.getTags().stream()
          .filter(tag -> tag.getName().equals(name))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Tag not found: " + name));
    }
  },
  schema {
    @Override
    public List<String> getArgumentNames() {
      return List.of("name");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var openApi = InternalContext.openApi(context);
      var name = args.get("name").toString();
      var path = name.split("\\.");
      var schema = openApi.getComponents().getSchemas().get(path[0]);
      if (schema == null) {
        throw new IllegalArgumentException("Schema not found: " + name);
      }
      for (int i = 1; i < path.length; i++) {
        Schema<?> inner = (Schema<?>) schema.getProperties().get(path[i]);
        if (inner == null) {
          throw new IllegalArgumentException(
              "Property not found: " + Stream.of(path).limit(i).collect(Collectors.joining(".")));
        }
        schema = inner;
      }

      return schema;
    }
  },
  operation {
    @Override
    public List<String> getArgumentNames() {
      return List.of("identifier", "pattern");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      try {
        var namedArgs = new HashMap<String, String>();
        var value = (String) args.get("identifier");
        if (isHTTPMethod(value)) {
          namedArgs.put("method", value);
        } else if (value.startsWith("/")) {
          namedArgs.put("pattern", value);
        } else {
          namedArgs.put("id", value);
        }
        namedArgs.putIfAbsent("pattern", (String) args.get("pattern"));
        var openApi = InternalContext.openApi(context);
        var operationId = namedArgs.get("id");
        if (operationId == null) {
          var method = namedArgs.get("method");
          var path = namedArgs.get("pattern");
          return openApi.findOperation(method, path);
        } else {
          return openApi.findOperationById(operationId);
        }
      } catch (Exception cause) {
        throw new PebbleException(
            cause, name() + " failed to generate output (?:?)", lineNumber, self.getName());
      }
    }

    private boolean isHTTPMethod(String value) {
      return switch (value.toUpperCase()) {
        case "GET" -> true;
        case "POST" -> true;
        case "PUT" -> true;
        case "DELETE" -> true;
        case "HEAD" -> true;
        case "OPTIONS" -> true;
        case "TRACE" -> true;
        case "PATCH" -> true;
        default -> false;
      };
    }
  };

  public static Map<String, Function> fn() {
    Map<String, Function> functions = new HashMap<>();
    for (Functions value : values()) {
      functions.put(value.name(), value);
    }
    return functions;
  }
}
