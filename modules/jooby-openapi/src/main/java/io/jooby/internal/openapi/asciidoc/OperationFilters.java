/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.CaseFormat;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.google.common.net.UrlEscapers;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.jooby.internal.openapi.OperationExt;
import io.jooby.internal.openapi.RequestBodyExt;
import io.jooby.internal.openapi.ResponseExt;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

public enum OperationFilters implements Filter {
  curl {
    private static final CharSequence Accept = new HeaderName("Accept");
    private static final CharSequence ContentType = new HeaderName("Content-Type");

    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      var options = args(args);
      var method = removeOption(options, "-X", operation.getMethod()).toUpperCase();
      var language = removeOption(options, "language", "");
      /* Accept/Content-Type: */
      var addAccept = true;
      var addContentType = true;
      if (options.containsKey("-H")) {
        var headers = parseHeaders(options.get("-H"));
        addAccept = !headers.containsKey(Accept);
        addContentType = !headers.containsKey(ContentType);
      }
      if (addAccept) {
        operation.getProduces().forEach(value -> options.put("-H", "'Accept: " + value + "'"));
      }
      if (addContentType && !READ_METHODS.contains(method)) {
        operation
            .getConsumes()
            .forEach(value -> options.put("-H", "'Content-Type: " + value + "'"));
      }
      var parameters = Optional.ofNullable(operation.getParameters()).orElse(List.of());
      /* Body */
      if (operation.getRequestBody() != null) {
        var requestBody = operation.getRequestBody();
        var content = requestBody.getContent();
        if (content != null) {
          var mediaType =
              content.get(operation.getConsumes().stream().findFirst().orElse(MediaType.JSON));
          if (mediaType != null) {
            var json = InternalContext.json(context);
            options.put(
                "-d",
                "'"
                    + json.writeValueAsString(
                        SchemaData.from(mediaType.getSchema(), schemaRefResolver(context)))
                    + "'");
          }
        }
      } else {
        // can be form
        var form = parameters.stream().filter(it -> "form".equals(it.getIn())).toList();
        encodeUrlParameter(form)
            .forEach(
                it ->
                    options.put(
                        it.getKey(),
                        it.getKey().equals("-F")
                            ? "\"" + it.getValue() + "\""
                            : "'" + it.getValue() + "'"));
      }
      /* Method */
      var url = snippetContext.get("url").toString();
      var query = parameters.stream().filter(it -> "query".equals(it.getIn())).toList();
      // query parameters
      if (!query.isEmpty()) {
        url +=
            encodeUrlParameter(query)
                .map(Map.Entry::getValue)
                .collect(Collectors.joining("&", "?", ""));
      }
      options.put("-X", method + " '" + url + "'");
      var optionsString = toString(options);
      snippetContext.put("options", optionsString);
      snippetContext.put("language", language);
      return resolver.apply(id(), snippetContext);
    }

    @NonNull private static String removeOption(
        Multimap<String, String> options, String name, String defaultValue) {
      return Optional.of(options.removeAll(name))
          .map(Collection::iterator)
          .filter(Iterator::hasNext)
          .map(Iterator::next)
          .orElse(defaultValue);
    }

    @NonNull private static Stream<Map.Entry<String, String>> encodeUrlParameter(List<Parameter> query) {
      return query.stream()
          .flatMap(
              it -> {
                var names = List.of(it.getName());
                var schema = it.getSchema();
                var index = new AtomicInteger(0);
                if (it.getSchema().getType().equals("array")) {
                  schema = it.getSchema().getItems();
                  // shows 3 examples
                  names = List.of(it.getName(), it.getName(), it.getName());
                  index.set(1);
                }
                var option = "binary".equals(schema.getFormat()) ? "-F" : "--data-urlencode";
                var value =
                    "binary".equals(schema.getFormat())
                        ? "@/file%1$s.extension"
                        : SchemaData.shemaType(schema) + "%1$s";
                return names.stream()
                    .map(
                        name ->
                            Map.entry(
                                option,
                                name
                                    + "="
                                    + String.format(
                                        value, (index.get() == 0 ? "" : index.getAndIncrement()))));
              });
    }
  },
  request {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      /* Body */
      var requestBodyString = "";
      if (operation.getRequestBody() != null) {
        var json = InternalContext.json(context);
        var schema = schema(context, operation.getRequestBody());
        requestBodyString =
            json.writeValueAsString(SchemaData.from(schema, schemaRefResolver(context))) + "\n";
      }
      snippetContext.put("requestBody", requestBodyString);
      snippetContext.put("headers", snippetContext.get("requestHeaders"));
      return resolver.apply(id(), snippetContext);
    }
  },
  requestFields {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      /* Body */
      List<Map<String, String>> fields = List.of();
      if (operation.getRequestBody() != null) {
        var schema = schema(context, operation.getRequestBody());
        fields = schemaToTable(schema, context);
      }
      snippetContext.put("fields", fields);
      return resolver.apply(id(), snippetContext);
    }
  },
  response {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      /* Body */
      var requestBodyString = "";
      var statusCode = findStatusCode(args);
      var response = responseByStatusCode(operation, statusCode);
      if (statusCode == null) {
        statusCode = StatusCode.valueOf(Integer.parseInt(response.getCode()));
      }
      var json = InternalContext.json(context);
      var schema = schema(context, response);
      if (schema != null) {
        requestBodyString =
            json.writeValueAsString(SchemaData.from(schema, schemaRefResolver(context))) + "\n";
      }
      snippetContext.put("statusCode", statusCode.value());
      snippetContext.put("statusReason", statusCode.reason());
      snippetContext.put("responseBody", requestBodyString);
      snippetContext.put("headers", snippetContext.get("responseHeaders"));
      return resolver.apply(id(), snippetContext);
    }
  },
  responseFields {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      /* Body */
      var statusCode = findStatusCode(args);
      var response = responseByStatusCode(operation, statusCode);
      var schema = schema(context, response);
      snippetContext.put("fields", schemaToTable(schema, context));
      return resolver.apply(id(), snippetContext);
    }
  },
  formParameters {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      snippetContext.put("parameters", parametersToTable(operation, p -> "form".equals(p.getIn())));
      return resolver.apply(id(), snippetContext);
    }
  },
  queryParameters {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      snippetContext.put(
          "parameters", parametersToTable(operation, p -> "query".equals(p.getIn())));
      return resolver.apply(id(), snippetContext);
    }
  },
  path {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber) {
      var namedArgs = positionalArgs(args);
      // Path
      var path = parameters(operation, p -> "path".equals(p.getIn()));
      var pathParams = new HashMap<String, Object>();
      path.forEach(
          p -> {
            pathParams.put(
                p.getName(), namedArgs.getOrDefault(p.getName(), "{" + p.getName() + "}"));
          });
      // QueryString
      var query =
          parameters(
              operation,
              p ->
                  "query".equals(p.getIn())
                      && (namedArgs.isEmpty() || namedArgs.containsKey(p.getName())));
      var queryString =
          query.isEmpty()
              ? ""
              : query.stream()
                  .map(
                      it -> {
                        var value =
                            namedArgs.getOrDefault(
                                it.getName(), SchemaData.shemaType(it.getSchema()));
                        return it.getName()
                            + "="
                            + UrlEscapers.urlFragmentEscaper().escape(value.toString());
                      })
                  .collect(Collectors.joining("&", "?", ""));
      return operation.getPath(pathParams) + queryString;
    }
  },
  pathParameters {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      snippetContext.put("parameters", parametersToTable(operation, p -> "path".equals(p.getIn())));
      return resolver.apply(id(), snippetContext);
    }
  },
  cookieParameters {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      snippetContext.put("cookies", parametersToTable(operation, p -> "cookie".equals(p.getIn())));
      return resolver.apply(id(), snippetContext);
    }
  },
  requestParameters {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      snippetContext.put("parameters", parametersToTable(operation, p -> true));
      return resolver.apply(id(), snippetContext);
    }
  },
  schema {
    @Override
    public String apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      try {
        var schema = schema(context, input);
        var snippetResolver = InternalContext.resolver(context);
        var json = InternalContext.json(context);
        return snippetResolver.apply(
            id(),
            Map.of(
                "schema",
                json.writer()
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(SchemaData.from(schema, schemaRefResolver(context)))));
      } catch (PebbleException pebbleException) {
        throw pebbleException;
      } catch (Exception exception) {
        throw new PebbleException(
            exception, name() + " failed to generate output", lineNumber, self.getName());
      }
    }

    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      // NOOP
      return null;
    }
  },
  statusCode {
    @Override
    protected String doApply(
        SnippetResolver resolver,
        OperationExt operation,
        Map<String, Object> snippetContext,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws Exception {
      var statusCode = findStatusCode(args);
      var response = responseByStatusCode(operation, statusCode, null);
      if (response == null) {
        throw new IllegalArgumentException("No response for: " + statusCode);
      }
      var json = InternalContext.json(context);
      var schema = schema(context, response);
      Map<String, Object> schemaData;
      if (schema == null) {
        if (statusCode.value() >= 400) {
          schemaData = new LinkedHashMap<>();
          // follow default error handler
          schemaData.put("message", "...");
          schemaData.put("statusCode", statusCode.value());
          schemaData.put("reason", statusCode.reason());
        } else {
          throw new IllegalArgumentException("No schema response for: " + statusCode);
        }
      } else {
        schemaData = SchemaData.from(schema, schemaRefResolver(context));
      }
      var responseString = json.writer().withDefaultPrettyPrinter().writeValueAsString(schemaData);
      snippetContext.put(
          "statusReason",
          Optional.ofNullable(response.getDescription()).orElse(statusCode.reason()));
      snippetContext.put("response", responseString);
      return resolver.apply(id(), snippetContext);
    }
  };

  protected final String id() {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name());
  }

  @Override
  public List<String> getArgumentNames() {
    return null;
  }

  protected abstract String doApply(
      SnippetResolver resolver,
      OperationExt operation,
      Map<String, Object> snippetContext,
      Map<String, Object> args,
      PebbleTemplate self,
      EvaluationContext context,
      int lineNumber)
      throws Exception;

  @Override
  public String apply(
      Object input,
      Map<String, Object> args,
      PebbleTemplate self,
      EvaluationContext context,
      int lineNumber)
      throws PebbleException {
    try {
      if (!(input instanceof OperationExt operation)) {
        throw new IllegalArgumentException(
            "Argument must be " + Operation.class.getName() + ". Got: " + input);
      }
      var snippetResolver = InternalContext.resolver(context);
      var snippetContext =
          newSnippetContext(InternalContext.variable(context, "serverUrl"), operation);
      return doApply(snippetResolver, operation, snippetContext, args, self, context, lineNumber);
    } catch (PebbleException pebbleException) {
      throw pebbleException;
    } catch (Exception exception) {
      throw new PebbleException(
          exception, name() + " failed to generate output", lineNumber, self.getName());
    }
  }

  protected ResponseExt responseByStatusCode(
      OperationExt operation, @Nullable StatusCode statusCode) {
    return responseByStatusCode(operation, statusCode, operation.getDefaultResponse());
  }

  protected ResponseExt responseByStatusCode(
      OperationExt operation, @Nullable StatusCode statusCode, ResponseExt defaultResponse) {
    ResponseExt response;
    if (statusCode != null) {
      response = (ResponseExt) operation.getResponses().get(Integer.toString(statusCode.value()));
      if (response == null) {
        throw new IllegalArgumentException("No response: " + statusCode.value());
      }
    } else {
      response = defaultResponse;
    }
    return response;
  }

  protected Map<String, Object> newSnippetContext(String serverUrl, OperationExt operation) {
    Map<String, Object> map = new HashMap<>();
    map.put("pattern", operation.getPath());
    map.put("path", operation.getPath());
    map.put("method", operation.getMethod());
    map.put("url", serverUrl + operation.getPath());
    var requestHeaders = ArrayListMultimap.<String, String>create();
    var responseHeaders = ArrayListMultimap.<String, String>create();
    var parameters = Optional.ofNullable(operation.getParameters()).orElse(List.of());
    var headerParams =
        parameters.stream().filter(it -> "header".equalsIgnoreCase(it.getIn())).toList();
    operation
        .getProduces()
        .forEach(
            value -> {
              requestHeaders.put("Accept", value);
              responseHeaders.put("Content-Type", value);
            });
    headerParams.forEach(it -> requestHeaders.put(it.getName(), "{{" + it.getName() + "}}"));
    if (!READ_METHODS.contains(operation.getMethod())) {
      operation.getConsumes().forEach(value -> requestHeaders.put("Content-Type", value));
    }

    Function<Map.Entry<String, String>, Map<String, String>> mapper =
        e -> Map.of("name", e.getKey(), "value", e.getValue());
    map.put("requestHeaders", requestHeaders.entries().stream().map(mapper).toList());
    map.put("responseHeaders", responseHeaders.entries().stream().map(mapper).toList());
    return map;
  }

  protected StatusCode findStatusCode(Map<String, Object> args) {
    for (var value : args.values()) {
      try {
        var code = Integer.parseInt(String.valueOf(value));
        if (code >= 100 && code <= 600) {
          return StatusCode.valueOf(code);
        }
      } catch (NumberFormatException ignored) {
      }
    }
    return null;
  }

  protected List<Map<String, String>> schemaToTable(Schema<?> schema, EvaluationContext context) {
    List<Map<String, String>> fields = new ArrayList<>();
    SchemaData.from(schema, schemaRefResolver(context))
        .forEach(
            (name, type) -> {
              var field = new LinkedHashMap<String, String>();
              field.put("name", name);
              field.put("type", type.toString());
              var property = schema.getProperties().get(name);
              if (property != null) {
                field.put("description", property.getDescription());
              }
              fields.add(field);
            });
    return fields;
  }

  protected List<Parameter> parameters(OperationExt operation, Predicate<Parameter> predicate) {
    return Optional.ofNullable(operation.getParameters()).orElse(List.of()).stream()
        .filter(predicate)
        .sorted(Comparator.comparing(Parameter::getName))
        .toList();
  }

  protected List<Map<String, String>> parametersToTable(
      OperationExt operation, Predicate<Parameter> predicate) {
    List<Map<String, String>> fields = new ArrayList<>();
    var parameters = parameters(operation, predicate);
    parameters.forEach(
        it -> {
          var schema = it.getSchema();
          var field = new LinkedHashMap<String, String>();
          field.put("name", it.getName());
          field.put("type", SchemaData.shemaType(schema));
          field.put("description", it.getDescription());
          field.put("in", it.getIn());
          fields.add(field);
        });
    return fields;
  }

  protected Schema<?> schema(EvaluationContext context, Object input) {
    var schema =
        switch (input) {
          case Schema<?> s -> s;
          case RequestBodyExt requestBody ->
              Optional.ofNullable(requestBody.getContent())
                  .flatMap(content -> content.values().stream().findFirst())
                  .map(io.swagger.v3.oas.models.media.MediaType::getSchema)
                  .orElse(null);
          case ResponseExt response ->
              Optional.ofNullable(response.getContent())
                  .flatMap(content -> content.values().stream().findFirst())
                  .map(io.swagger.v3.oas.models.media.MediaType::getSchema)
                  .orElse(null);
          case null -> throw new NullPointerException("Unable to get schema from null");
          default ->
              throw new IllegalArgumentException(
                  "Unable to get schema from " + input.getClass().getName());
        };
    if (schema != null && schema.get$ref() != null) {
      return schemaRefResolver(context).apply(schema.get$ref()).orElse(schema);
    }
    return schema;
  }

  @SuppressWarnings("unchecked")
  protected Function<String, Optional<Schema<?>>> schemaRefResolver(EvaluationContext context) {
    var openapi = InternalContext.openApi(context);
    return ref -> {
      var name = ref.substring("#/components/schemas/".length());
      var components = openapi.getComponents();
      if (components != null) {
        return Optional.ofNullable(components.getSchemas().get(name));
      }
      return Optional.empty();
    };
  }

  protected Multimap<CharSequence, String> parseHeaders(Collection<String> headers) {
    Multimap<CharSequence, String> result = LinkedHashMultimap.create();
    for (var line : headers) {
      if (line.startsWith("'") && line.endsWith("'")) {
        line = line.substring(1, line.length() - 1);
      }
      var header = Splitter.on(':').trimResults().omitEmptyStrings().splitToList(line);
      if (header.size() != 2) {
        throw new IllegalArgumentException("Invalid header: " + line);
      }
      result.put(new HeaderName(header.get(0)), header.get(1));
    }
    return result;
  }

  protected static final Set<String> READ_METHODS = Set.of("GET", "HEAD");

  protected String toString(Multimap<String, String> options) {
    var sb = new StringBuilder();
    var separator = "\\\n";
    var tabSize = id().length() + 1;
    for (Map.Entry<String, String> entry : options.entries()) {
      var k = entry.getKey();
      var v = entry.getValue();
      if (!sb.isEmpty()) {
        sb.append(" ".repeat(tabSize));
      }
      sb.append(k);
      if (v != null && !v.isEmpty()) {
        sb.append(" ").append(v);
      }
      sb.append(separator);
    }
    sb.setLength(sb.length() - separator.length());
    return sb.toString();
  }

  protected Map<String, Object> positionalArgs(Map<String, Object> args) {
    var optionList = new ArrayList<>(args.values());
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < optionList.size(); i += 2) {
      var key = optionList.get(i).toString();
      var value = optionList.get(i + 1);
      result.put(key, value);
    }
    return result;
  }

  protected Multimap<String, String> args(Map<String, Object> args) {
    Multimap<String, String> result = LinkedHashMultimap.create();
    var optionList = new ArrayList<>(args.values());
    for (int i = 0; i < optionList.size(); ) {
      var key = optionList.get(i).toString();
      String value = null;
      if (i + 1 < optionList.size()) {
        var next = optionList.get(i + 1);
        if (next.toString().startsWith("-")) {
          i += 1;
        } else {
          value = next.toString();
          i += 2;
        }
      } else {
        i += 1;
      }
      result.put(key, value == null ? "" : value);
    }
    return result;
  }

  protected record HeaderName(String value) implements CharSequence {

    @Override
    public int length() {
      return value.length();
    }

    @Override
    public boolean equals(Object obj) {
      return value.equalsIgnoreCase(obj.toString());
    }

    @Override
    public int hashCode() {
      return value.toLowerCase().hashCode();
    }

    @Override
    public char charAt(int index) {
      return value.charAt(index);
    }

    @NonNull @Override
    public CharSequence subSequence(int start, int end) {
      return value.subSequence(start, end);
    }

    @Override
    @NonNull public String toString() {
      return value;
    }
  }
}
