/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import io.jooby.annotations.CONNECT;
import io.jooby.annotations.Consumes;
import io.jooby.annotations.CookieParam;
import io.jooby.annotations.DELETE;
import io.jooby.annotations.FlashParam;
import io.jooby.annotations.FormParam;
import io.jooby.annotations.GET;
import io.jooby.annotations.HEAD;
import io.jooby.annotations.HeaderParam;
import io.jooby.annotations.OPTIONS;
import io.jooby.annotations.PATCH;
import io.jooby.annotations.POST;
import io.jooby.annotations.PUT;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.jooby.annotations.Produces;
import io.jooby.annotations.QueryParam;
import io.jooby.annotations.TRACE;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

public interface Annotations {
  Set<String> HTTP_METHODS = unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
      GET.class.getName(),
      javax.ws.rs.GET.class.getName(),

      POST.class.getName(),
      javax.ws.rs.POST.class.getName(),

      PUT.class.getName(),
      javax.ws.rs.PUT.class.getName(),

      DELETE.class.getName(),
      javax.ws.rs.DELETE.class.getName(),

      PATCH.class.getName(),
      javax.ws.rs.PATCH.class.getName(),

      HEAD.class.getName(),
      javax.ws.rs.HEAD.class.getName(),

      OPTIONS.class.getName(),
      javax.ws.rs.OPTIONS.class.getName(),

      CONNECT.class.getName(),

      TRACE.class.getName()
  )));

  Set<String> PATH_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(PathParam.class.getName())));

  Set<String> QUERY_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      QueryParam.class.getName(),
      javax.ws.rs.QueryParam.class.getName()
  )));

  Set<String> COOKIE_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      CookieParam.class.getName(),
      javax.ws.rs.CookieParam.class.getName()
  )));

  Set<String> HEADER_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      HeaderParam.class.getName(),
      javax.ws.rs.HeaderParam.class.getName()
  )));

  Set<String> FLASH_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(FlashParam.class.getName())));

  Set<String> FORM_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      FormParam.class.getName(),
      javax.ws.rs.FormParam.class.getName()
  )));

  Set<String> PRODUCES_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      Produces.class.getName(),
      javax.ws.rs.Produces.class.getName()
  )));

  Set<String> CONSUMES_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      Consumes.class.getName(),
      javax.ws.rs.Consumes.class.getName()
  )));

  Set<String> PATH = unmodifiableSet(new LinkedHashSet<>(asList(
      Path.class.getName(),
      javax.ws.rs.Path.class.getName()
  )));

  static List<String> attribute(AnnotationMirror mirror, String name) {
    Function<Object, String> cleanValue = arg -> {
      if (arg instanceof AnnotationValue) {
        return ((AnnotationValue) arg).getValue().toString();
      }
      return arg.toString();
    };

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
        .getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().toString().equals(name)) {
        Object value = entry.getValue().getValue();
        if (value instanceof List) {
          List values = (List) value;
          return (List<String>) values.stream()
              .map(cleanValue)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
        }
        String singleValue = cleanValue.apply(value);
        return singleValue == null
            ? Collections.emptyList()
            : Collections.singletonList(singleValue);
      }
    }
    return Collections.emptyList();
  }
}
