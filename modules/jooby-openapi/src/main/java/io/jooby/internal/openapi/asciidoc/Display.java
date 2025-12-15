/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.*;

import io.jooby.internal.openapi.OperationExt;
import io.jooby.internal.openapi.asciidoc.display.*;
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
      var asciidoc = AsciiDocContext.from(context);
      var pretty = args.getOrDefault("pretty", true) == Boolean.TRUE;
      return wrap(
          asciidoc.toJson(toJson(asciidoc, input), pretty),
          args.getOrDefault("wrap", Boolean.TRUE) == Boolean.TRUE,
          "[source, json]\n----\n",
          "\n----");
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("wrap");
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
      var asciidoc = AsciiDocContext.from(context);
      return wrap(
          asciidoc.toYaml(toJson(asciidoc, input)),
          args.getOrDefault("wrap", Boolean.TRUE) == Boolean.TRUE,
          "[source, yaml]\n----\n",
          "\n----");
    }

    @Override
    public List<String> getArgumentNames() {
      return List.of("wrap");
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
      var asciidoc = AsciiDocContext.from(context);
      return new SafeString(toAsciidoc(asciidoc, input).table(new TreeMap<>(args)));
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
      var asciidoc = AsciiDocContext.from(context);
      return new SafeString(toAsciidoc(asciidoc, input).list(new TreeMap<>(args)));
    }
  },
  link {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var schema =
          switch (input) {
            case Schema<?> s -> s;
            case HttpMessage msg -> msg.getBody();
            default -> throw new IllegalArgumentException("Can't render: " + input);
          };
      var asciidoc = AsciiDocContext.from(context);
      var resolved = asciidoc.resolveSchema(schema);
      var target = resolved;
      var prefix = "";
      var suffix = "";
      if (resolved.getItems() != null) {
        target = asciidoc.resolveSchema(resolved.getItems());
        prefix = Optional.ofNullable(resolved.getName()).orElse("") + "[";
        suffix = "]";
      }
      if ("object".equals(asciidoc.resolveType(target))) {
        return new SafeString(prefix + "<<" + target.getName() + ">>" + suffix);
      }
      // no link for basic types
      return prefix + target.getName() + suffix;
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
      var asciidoc = AsciiDocContext.from(context);
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
  path {
    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      var asciidoc = AsciiDocContext.from(context);
      var request =
          switch (input) {
            case OperationExt op -> new HttpRequest(asciidoc, op, args);
            case HttpRequest req -> req;
            default -> throw new IllegalArgumentException("Can't render: " + input);
          };
      var pathParams = new HashMap<String, Object>();
      request
          .getParameters(List.of("path"), List.of())
          .forEach(
              p -> {
                pathParams.put(
                    p.getName(), args.getOrDefault(p.getName(), "{" + p.getName() + "}"));
              });
      // QueryString
      pathParams.keySet().forEach(args::remove);
      var queryString = request.getQueryString(args);
      return request.operation().getPath(pathParams) + queryString;
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
      var asciidoc = AsciiDocContext.from(context);
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
      case HttpRequest req -> OpenApiToAsciiDoc.parameters(context, req.getAllParameters());
      case HttpResponse rsp -> OpenApiToAsciiDoc.schema(context, rsp.getBody());
      case Schema<?> schema -> OpenApiToAsciiDoc.schema(context, schema);
      case ParameterList paramList -> OpenApiToAsciiDoc.parameters(context, paramList);
      case ToAsciiDoc asciiDoc -> asciiDoc;
      case Map map -> new MapToAsciiDoc(List.of(map));
      default -> throw new IllegalArgumentException("Can't render: " + input);
    };
  }

  protected Object toJson(AsciiDocContext context, Object input) {
    return switch (input) {
      case Schema<?> schema -> context.schemaProperties(schema);
      case HttpResponse rsp -> toJson(context, rsp.getSucessOrError());
      case StatusCodeList codeList ->
          codeList.codes().size() == 1 ? codeList.codes().getFirst() : codeList.codes();
      default -> input;
    };
  }

  protected SafeString wrap(String content, boolean wrap, String prefix, String suffix) {
    return new SafeString(wrap ? prefix + content + suffix : content);
  }

  @Override
  public List<String> getArgumentNames() {
    return List.of();
  }
}
