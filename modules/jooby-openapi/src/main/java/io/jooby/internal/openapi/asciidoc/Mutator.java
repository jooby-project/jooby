/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jooby.internal.openapi.OperationExt;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;

public enum Mutator implements Filter {
  example {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      if (input instanceof Schema<?> schema) {
        var asciidoc = AsciiDocContext.from(context);
        return asciidoc.schemaExample(schema);
      }
      return input;
    }
  },
  truncate {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      if (input instanceof Schema<?> schema) {
        var asciidoc = AsciiDocContext.from(context);
        return asciidoc.reduceSchema(schema);
      }
      return input;
    }
  },
  request {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      return new HttpRequest(AsciiDocContext.from(context), toOperation(input), args);
    }
  },
  response {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      return new HttpResponse(
          AsciiDocContext.from(context),
          toOperation(input),
          Optional.ofNullable(args.get("code"))
              .map(Number.class::cast)
              .map(Number::intValue)
              .orElse(null),
          args);
    }
  },
  parameters {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var in = normalizeList(args.getOrDefault("in", "*"));
      var includes = normalizeList(args.getOrDefault("includes", List.of()));
      return toHttpRequest(context, input, args).getParameters(in, includes);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<String> normalizeList(Object value) {
      if (value instanceof List valueList) {
        return valueList;
      }
      return value == null ? List.of() : List.of(value.toString());
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("in", "includes");
    }
  },
  body {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var bodyType = args.getOrDefault("type", "full");
      return toHttpMessage(context, input, Map.of("body", bodyType)).getBody();
    }
  },
  form {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      return toHttpRequest(context, input, args).getForm();
    }
  };

  protected OperationExt toOperation(Object input) {
    if (!(input instanceof OperationExt)) {
      throw new IllegalArgumentException(
          "Not an operation: " + input.getClass() + ", expecting: " + Operation.class);
    }
    return (OperationExt) input;
  }

  protected HttpMessage toHttpMessage(
      EvaluationContext context, Object input, Map<String, Object> options) {
    return switch (input) {
      case null -> throw new NullPointerException(name() + ": requires a request/response input");
      // default to http request
      case OperationExt op -> new HttpRequest(AsciiDocContext.from(context), op, options);
      case HttpMessage msg -> msg;
      default ->
          throw new ClassCastException(
              name() + ": requires a request/response input: " + input.getClass());
    };
  }

  protected HttpRequest toHttpRequest(
      EvaluationContext context, Object input, Map<String, Object> options) {
    return switch (input) {
      case null -> throw new NullPointerException(name() + ": requires a request/response input");
      // default to http request
      case OperationExt op -> new HttpRequest(AsciiDocContext.from(context), op, options);
      case HttpRequest msg -> msg;
      default ->
          throw new ClassCastException(
              name() + ": requires a request/response input: " + input.getClass());
    };
  }

  @Override
  public List<String> getArgumentNames() {
    return List.of();
  }
}
