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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.net.UrlEscapers;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Router;
import io.jooby.internal.openapi.OperationExt;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

public record HttpRequest(
    AsciiDocContext context, OperationExt operation, Map<String, Object> options)
    implements HttpMessage {

  private static final Predicate<Parameter> NOOP = p -> true;

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
  public HttpParamList getHeaders() {
    var requestHeaders = ArrayListMultimap.<String, HttpParam>create();
    var parameters = Optional.ofNullable(operation.getParameters()).orElse(List.of());
    var headerParams = parameters.stream().filter(it -> "header".equals(it.getIn())).toList();
    operation
        .getProduces()
        .forEach(
            value ->
                requestHeaders.put(
                    "Accept", new HttpParam("Accept", new StringSchema(), value, "header", null)));
    if (Set.of(Router.PATCH, Router.PUT, Router.POST, Router.DELETE)
        .contains(operation.getMethod())) {
      operation
          .getConsumes()
          .forEach(
              value ->
                  requestHeaders.put(
                      "Content-Type",
                      new HttpParam("Content-Type", new StringSchema(), value, "header", null)));
    }
    headerParams.forEach(
        it ->
            requestHeaders.put(
                it.getName(),
                new HttpParam(
                    it.getName(), it.getSchema(), "{{" + it.getName() + "}}", "header", null)));
    return new HttpParamList(
        requestHeaders.entries().stream().map(Map.Entry::getValue).toList(),
        HttpParamList.NAME_DESC);
  }

  @Override
  public HttpParamList getCookies() {
    var parameters = Optional.ofNullable(operation.getParameters()).orElse(List.of());
    return new HttpParamList(
        parameters.stream()
            .filter(it -> "cookie".equals(it.getIn()))
            .map(
                it ->
                    new HttpParam(
                        it.getName(),
                        it.getSchema(),
                        "{{" + it.getName() + "}}",
                        "cookie",
                        it.getDescription()))
            .toList(),
        HttpParamList.NAME_DESC);
  }

  public HttpParamList getParameters() {
    return getParameterList(NOOP, Map.of(), HttpParamList.PARAM);
  }

  public HttpParamList getQueryParameters() {
    return getQueryParameters(Map.of());
  }

  public String getQueryString() {
    var sb = new StringBuilder("?");

    for (var param : getParameters(inFilter("query"), Map.of())) {
      encode(
          param.getName(),
          param.getSchema(),
          (schema, e) ->
              Map.entry(e.getKey(), UrlEscapers.urlFragmentEscaper().escape(e.getValue())),
          (name, value) -> sb.append(name).append("=").append(value).append("&"));
    }
    if (sb.length() > 1) {
      sb.setLength(sb.length() - 1);
      return sb.toString();
    }
    return "";
  }

  private HttpParamList getQueryParameters(Map<String, Object> paramValues) {
    return getParameterList(inFilter("query"), paramValues, HttpParamList.NAME_TYPE_DESC);
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

  public ListMultimap<String, String> getFormUrlEncoded() {
    return formUrlEncoded((schema, field) -> field);
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

  public HttpParamList getPathParameters() {
    return getParameterList(inFilter("path"), Map.of(), HttpParamList.NAME_TYPE_DESC);
  }

  public HttpParamList getAllParameters() {
    var parameters = new ArrayList<>(getParameters(NOOP, Map.of()));
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
    return toParameterList(parameters, Map.of(), HttpParamList.PARAM);
  }

  private HttpParamList getParameterList(
      Predicate<Parameter> predicate, Map<String, Object> paramValues, List<String> includes) {
    return toParameterList(getParameters(predicate, paramValues), paramValues, includes);
  }

  private HttpParamList toParameterList(
      List<Parameter> parameters, Map<String, Object> paramValues, List<String> includes) {
    return new HttpParamList(
        parameters.stream()
            .map(
                it ->
                    new HttpParam(
                        it.getName(),
                        context.resolveSchema(it.getSchema()),
                        paramValues.get(it.getName()),
                        it.getIn(),
                        it.getDescription()))
            .toList(),
        includes);
  }

  private List<Parameter> getParameters(
      Predicate<Parameter> predicate, Map<String, Object> paramValues) {
    var parameters = Optional.ofNullable(operation.getParameters()).orElse(List.of());
    return parameters.stream().filter(predicate.and(paramValueFilter(paramValues))).toList();
  }

  private static Predicate<Parameter> paramValueFilter(Map<String, Object> paramValues) {
    if (paramValues == null || paramValues.isEmpty()) {
      return NOOP;
    }
    return p -> paramValues.containsKey(p.getName());
  }

  private static Predicate<Parameter> inFilter(String in) {
    return p -> in.equals(p.getIn());
  }

  @NonNull @Override
  public String toString() {
    return getMethod() + " " + getPath();
  }

  public String getSummary() {
    return operation.getSummary();
  }
}
