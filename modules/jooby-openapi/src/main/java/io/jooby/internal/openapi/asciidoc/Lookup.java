/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.*;

import io.jooby.StatusCode;
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
  },
  error {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var error = context.getVariable("error");
      if (error == null) {
        // mimic default error handler
        Map<String, Object> defaultError = new TreeMap<>();
        var statusCode =
            StatusCode.valueOf(
                ((Number)
                        args.getOrDefault(
                            "code",
                            args.getOrDefault("statusCode", StatusCode.SERVER_ERROR.value())))
                    .intValue());
        defaultError.put("statusCode", statusCode.value());
        defaultError.put(
            "reason",
            args.getOrDefault(
                "reason", args.getOrDefault("statusCodeReason", statusCode.reason())));
        defaultError.put("message", args.getOrDefault("message", "..."));
        return defaultError;
      } else if (error instanceof Map<?, ?> errorMap) {
        var mutableMap = new TreeMap<String, Object>();
        mutableMap.putAll((Map<? extends String, ?>) errorMap);
        mutableMap.putAll(args);
        for (var entry : errorMap.entrySet()) {
          var value = entry.getValue();
          var template = String.valueOf(value);
          if (template.startsWith("{{") && template.endsWith("}}")) {
            var variable = template.substring(2, template.length() - 2).trim();
            value =
                switch (variable) {
                  case "status.reason",
                      "statusCodeReason",
                      "code.reason",
                      "codeReason",
                      "reason" -> {
                    var statusCode =
                        StatusCode.valueOf(
                            ((Number)
                                    args.getOrDefault(
                                        "code",
                                        args.getOrDefault(
                                            "statusCode", StatusCode.SERVER_ERROR.value())))
                                .intValue());
                    yield statusCode.reason();
                  }
                  default -> Optional.ofNullable(context.getVariable(variable)).orElse(template);
                };
            mutableMap.put((String) entry.getKey(), value);
          }
        }
        return mutableMap;
      }
      throw new ClassCastException("Global error must be a map: " + error);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of();
    }
  },
  server {
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var asciidoc = AsciiDocContext.from(context);
      var servers = asciidoc.getOpenApi().getServers();
      if (servers == null || servers.isEmpty()) {
        throw new NoSuchElementException("No servers");
      }
      var nameOrIndex = args.get("name");
      if (nameOrIndex instanceof Number index) {
        if (index.intValue() >= 0 && index.intValue() < servers.size()) {
          return servers.get(index.intValue());
        } else {
          throw new NoSuchElementException("Server not found: [" + nameOrIndex + "]");
        }
      } else {
        return servers.stream()
            .filter(it -> nameOrIndex.equals(it.getDescription()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Server not found: " + nameOrIndex));
      }
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
}
