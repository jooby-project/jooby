/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jooby.internal.openapi.OperationExt;
import io.jooby.internal.openapi.asciidoc.http.RequestToCurl;
import io.jooby.internal.openapi.asciidoc.http.RequestToHttp;
import io.jooby.internal.openapi.asciidoc.http.ResponseToHttp;
import io.jooby.internal.openapi.asciidoc.table.ToAsciiDoc;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.swagger.v3.oas.models.media.Schema;

public enum Display implements Filter {
  json {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var asciidoc = InternalContext.asciidoc(context);
      var pretty = args.getOrDefault("pretty", true) == Boolean.TRUE;
      return new SafeString(asciidoc.toJson(toJson(asciidoc, input), pretty));
    }
  },
  yaml {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var asciidoc = InternalContext.asciidoc(context);
      return new SafeString(asciidoc.toYaml(toJson(asciidoc, input)));
    }
  },
  table {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var asciidoc = InternalContext.asciidoc(context);
      return new SafeString(toAsciidoc(asciidoc, input).table(args));
    }
  },
  list {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var asciidoc = InternalContext.asciidoc(context);
      return new SafeString(toAsciidoc(asciidoc, input).list(args));
    }
  },
  curl {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var asciidoc = InternalContext.asciidoc(context);
      var curl =
          switch (input) {
            case OperationExt op ->
                new RequestToCurl(asciidoc, new HttpRequest(asciidoc, op, args));
            case HttpRequest req -> new RequestToCurl(asciidoc, req);
            default -> throw new IllegalArgumentException("Can't render: " + input);
          };
      return curl.render(args);
    }
  },
  http {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var asciidoc = InternalContext.asciidoc(context);
      return toHttp(asciidoc, input, args).render(args);
    }

    private ToSnippet toHttp(AsciiDocContext context, Object input, Map<String, Object> options) {
      return switch (input) {
        case OperationExt op -> new RequestToHttp(context, new HttpRequest(context, op, options));
        case HttpRequest req -> new RequestToHttp(context, req);
        case HttpResponse rsp -> new ResponseToHttp(context, rsp);
        default -> throw new IllegalArgumentException("Can't render: " + input);
      };
    }
  };

  protected ToAsciiDoc toAsciidoc(AsciiDocContext context, Object input) {
    return switch (input) {
      case HttpRequest req -> ToAsciiDoc.parameters(context, req.getAllParameters());
      case HttpResponse rsp -> ToAsciiDoc.schema(context, rsp.getBody());
      case Schema<?> schema -> ToAsciiDoc.schema(context, schema);
      case HttpParamList paramList -> ToAsciiDoc.parameters(context, paramList);
      default -> throw new IllegalArgumentException("Can't render: " + input);
    };
  }

  protected Object toJson(AsciiDocContext context, Object input) {
    return switch (input) {
      case Schema<?> schema -> context.schemaProperties(schema);
      default -> input;
    };
  }

  @Override
  public List<String> getArgumentNames() {
    return List.of();
  }

  public static Map<String, Filter> display() {
    Map<String, Filter> result = new HashMap<>();
    for (var value : values()) {
      result.put(value.name(), value);
    }
    return result;
  }
}
