/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.net.UrlEscapers;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Router;
import io.jooby.internal.openapi.OperationExt;
import io.jooby.internal.openapi.ParameterExt;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

@JsonIncludeProperties({"path", "method"})
public record HttpRequest(
    AsciiDocContext context, OperationExt operation, Map<String, Object> options)
    implements HttpMessage {

  private static final Predicate<Parameter> NOOP = p -> true;

  private List<Parameter> allParameters() {
    var parameters = new ArrayList<>(getImplicitHeaders());
    parameters.addAll(Optional.ofNullable(operation.getParameters()).orElse(List.of()));
    return parameters;
  }

  private List<Parameter> getImplicitHeaders() {
    var implicitHeaders = new ArrayList<Parameter>();
    operation
        .getProduces()
        .forEach(value -> implicitHeaders.add(ParameterExt.header("Accept", value)));
    if (Set.of(Router.PATCH, Router.PUT, Router.POST, Router.DELETE)
        .contains(operation.getMethod())) {
      operation
          .getConsumes()
          .forEach(value -> implicitHeaders.add(ParameterExt.header("Content-Type", value)));
    }
    return implicitHeaders;
  }

  public String getMethod() {
    return operation.getMethod();
  }

  public String getPath() {
    return operation.getPath();
  }

  public List<String> getProduces() {
    return operation.getProduces();
  }

  public List<String> getConsumes() {
    return operation.getConsumes();
  }

  @Override
  public ParameterList getHeaders() {
    return new ParameterList(
        allParameters().stream().filter(inFilter("header")).toList(), ParameterList.NAME_DESC);
  }

  @Override
  public ParameterList getCookies() {
    return new ParameterList(
        allParameters().stream().filter(inFilter("cookie")).toList(), ParameterList.NAME_DESC);
  }

  public ParameterList getQuery() {
    return new ParameterList(
        allParameters().stream().filter(inFilter("query")).toList(), ParameterList.NAME_TYPE_DESC);
  }

  public ParameterList getParameters() {
    return getParameterList(NOOP, ParameterList.PARAM);
  }

  public ParameterList getParameters(List<String> in, List<String> includes) {
    var show =
        in.isEmpty() || in.contains("*")
            ? ParameterList.PARAM
            : (in.size() == 1 && in.contains("cookie") || in.contains("header"))
                ? ParameterList.NAME_DESC
                : ParameterList.NAME_TYPE_DESC;
    return getParameterList(toFilter(in, includes), show);
  }

  private Predicate<Parameter> toFilter(List<String> in, List<String> includes) {
    Predicate<Parameter> inFilter;
    if (in.isEmpty()) {
      inFilter = NOOP;
    } else {
      inFilter = null;
      for (var type : in) {
        var itFilter = inFilter(type);
        if (inFilter == null) {
          inFilter = itFilter;
        } else {
          inFilter = inFilter.or(itFilter);
        }
      }
    }
    Predicate<Parameter> paramFilter = NOOP;
    if (!includes.isEmpty()) {
      paramFilter = p -> includes.contains(p.getName());
    }
    return inFilter.and(paramFilter);
  }

  public String getQueryString() {
    return getQueryString(Map.of());
  }

  public String getQueryString(Map<String, Object> filter) {
    var sb = new StringBuilder("?");

    for (var param : getParameters(List.of("query"), filter.keySet().stream().toList())) {
      encode(
          param.getName(),
          param.getSchema(),
          (schema, e) ->
              Map.entry(
                  e.getKey(),
                  UrlEscapers.urlFragmentEscaper()
                      .escape(filter.getOrDefault(e.getKey(), e.getValue()).toString())),
          (name, value) -> sb.append(name).append("=").append(value).append("&"));
    }
    if (sb.length() > 1) {
      sb.setLength(sb.length() - 1);
      return sb.toString();
    }
    return "";
  }

  @SuppressWarnings("unchecked")
  private Schema<?> getBody(List<String> contentType) {
    var body =
        Optional.ofNullable(operation.getRequestBody())
            .map(it -> toSchema(it.getContent(), contentType))
            .map(context::resolveSchema)
            .orElse(AsciiDocContext.EMPTY_SCHEMA);

    return selectBody(body, options.getOrDefault("body", "full").toString());
  }

  public Schema<?> getForm() {
    return getBody(List.of("application/x-www-form-urlencoded)", "multipart/form-data"));
  }

  @NonNull public ListMultimap<String, String> formUrlEncoded(
      BiFunction<Schema<?>, Map.Entry<String, String>, Map.Entry<String, String>> formatter) {
    var output = ArrayListMultimap.<String, String>create();
    var form = getForm();
    if (form != AsciiDocContext.EMPTY_SCHEMA) {
      traverseSchema(null, form, formatter, output::put);
    }
    return output;
  }

  private void traverseSchema(
      String path,
      Schema<?> schema,
      BiFunction<Schema<?>, Map.Entry<String, String>, Map.Entry<String, String>> formatter,
      BiConsumer<String, String> consumer) {
    context.traverseSchema(
        schema,
        (propertyName, value) -> {
          var propertyPath = path == null ? propertyName : path + "." + propertyName;
          if (value.getType().equals("object")) {
            traverseSchema(propertyPath, value, formatter, consumer);
          } else if (value.getType().equals("array")) {
            traverseSchema(propertyPath + "[0]", value.getItems(), formatter, consumer);
          } else {
            encode(propertyPath, value, formatter, consumer);
          }
        });
  }

  private void encode(
      String propertyName,
      Schema<?> schema,
      BiFunction<Schema<?>, Map.Entry<String, String>, Map.Entry<String, String>> formatter,
      BiConsumer<String, String> consumer) {
    var names = List.of(propertyName);
    var index = new AtomicInteger(0);
    if (schema.getType().equals("array")) {
      schema = schema.getItems();
      // shows 3 examples
      names = List.of(propertyName, propertyName, propertyName);
      index.set(1);
    }
    var schemaType = context.schemaType(schema);
    if ("binary".equals(schema.getFormat())) {
      schemaType = "file";
    }
    var value = schemaType + "%1$s";
    for (String name : names) {
      var formattedPair =
          formatter.apply(
              schema,
              Map.entry(
                  name, String.format(value, (index.get() == 0 ? "" : index.getAndIncrement()))));
      consumer.accept(formattedPair.getKey(), formattedPair.getValue());
    }
  }

  @Override
  public Schema<?> getBody() {
    return getBody(List.of());
  }

  public ParameterList getAllParameters() {
    var parameters = allParameters();
    var body = getForm();
    var bodyType = "form";
    if (body == AsciiDocContext.EMPTY_SCHEMA) {
      body = getBody();
      bodyType = "body";
    }
    var paramType = bodyType;
    if (body != AsciiDocContext.EMPTY_SCHEMA) {
      context.traverseSchema(
          body,
          (propertyName, schema) -> {
            var p = new Parameter();
            p.setName(propertyName);
            p.setSchema(schema);
            p.setIn(paramType);
            p.setDescription(schema.getDescription());
            parameters.add(p);
          });
    }
    return new ParameterList(parameters, ParameterList.PARAM);
  }

  private ParameterList getParameterList(Predicate<Parameter> predicate, List<String> includes) {
    return new ParameterList(getParameters(predicate), includes);
  }

  private List<Parameter> getParameters(Predicate<Parameter> predicate) {
    return predicate == NOOP
        ? allParameters()
        : allParameters().stream().filter(predicate).toList();
  }

  private static Predicate<Parameter> inFilter(String in) {
    return p -> "*".equals(in) || in.equals(p.getIn());
  }

  @NonNull @Override
  public String toString() {
    return getMethod() + " " + getPath();
  }

  public String getSummary() {
    return operation.getSummary();
  }
}
