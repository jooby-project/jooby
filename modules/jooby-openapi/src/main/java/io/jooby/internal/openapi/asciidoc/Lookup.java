/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.*;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
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

    @Override
    public List<String> alias() {
      return List.of("schema", "model");
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
          .map(
              it ->
                  new TagExt(
                      it,
                      asciidoc.getOpenApi().findOperationByTag(it.getName()).stream()
                          .map(op -> new HttpRequest(asciidoc, op, Map.of()))
                          .toList()))
          .orElseThrow(() -> new NoSuchElementException("Tag not found: " + name));
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("name");
    }
  },
  error {
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var asciidoc = AsciiDocContext.from(context);
      return asciidoc.error(context, args);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("code");
    }
  },
  routes {
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var asciidoc = AsciiDocContext.from(context);
      var operations = asciidoc.getOpenApi().getOperations();
      var list =
          operations.stream()
              .filter(
                  it -> {
                    var includes = (String) args.get("includes");
                    return includes == null || it.getPath().matches(includes);
                  })
              .map(it -> new HttpRequest(asciidoc, it, args))
              .toList();
      return new HttpRequestList(asciidoc, list);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("includes");
    }

    @Override
    public List<String> alias() {
      return List.of("routes", "operations");
    }
  },
  statusCode {
    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
      var code = args.get("code");
      if (code instanceof List<?> codes) {
        return new StatusCodeList(codes.stream().flatMap(this::toMap).toList());
      }
      return new StatusCodeList(toMap(code).toList());
    }

    @NonNull private Stream<Map<String, Object>> toMap(Object candidate) {
      if (candidate instanceof Number code) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code.intValue());
        map.put("reason", StatusCode.valueOf(code.intValue()).reason());
        return Stream.of(map);
      } else if (candidate instanceof Map<?, ?> codeMap) {
        var codes = new ArrayList<Map<String, Object>>();
        for (var entry : new TreeMap<>(codeMap).entrySet()) {
          var value = entry.getKey();
          if (value instanceof Number code) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", code.intValue());
            map.put("reason", entry.getValue());
            codes.add(map);
          } else {
            throw new ClassCastException("Must be Map<Number, String>: " + candidate);
          }
        }
        return codes.stream();
      }
      throw new ClassCastException("Not a number: " + candidate);
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("code");
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

  public List<String> alias() {
    return List.of(name());
  }

  protected Map<String, Object> appendMethod(Map<String, Object> args) {
    Map<String, Object> result = new LinkedHashMap<>(args);
    result.put("method", name());
    return result;
  }
}
