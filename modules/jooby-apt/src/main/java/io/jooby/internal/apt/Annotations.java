/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

/**
 * Annotation constants used by the APT.
 *
 * @since 2.1.0
 */
public interface Annotations {

  String CONNECT = "io.jooby.annotations.CONNECT";
  String Consumes = "io.jooby.annotations.Consumes";
  String ContextParam = "io.jooby.annotations.ContextParam";
  String DELETE = "io.jooby.annotations.DELETE";
  String FlashParam = "io.jooby.annotations.FlashParam";
  String FormParam = "io.jooby.annotations.FormParam";
  String CookieParam = "io.jooby.annotations.CookieParam";
  String GET = "io.jooby.annotations.GET";
  String HEAD = "io.jooby.annotations.HEAD";
  String HeaderParam = "io.jooby.annotations.HeaderParam";
  String OPTIONS = "io.jooby.annotations.OPTIONS";
  String PATCH = "io.jooby.annotations.PATCH";
  String POST = "io.jooby.annotations.POST";
  String PUT = "io.jooby.annotations.PUT";
  String Param = "io.jooby.annotations.Param";
  String Path = "io.jooby.annotations.Path";
  String PathParam = "io.jooby.annotations.PathParam";
  String Produces = "io.jooby.annotations.Produces";
  String QueryParam = "io.jooby.annotations.QueryParam";
  String SessionParam = "io.jooby.annotations.SessionParam";
  String TRACE = "io.jooby.annotations.TRACE";

  /** JAXRS GET. */
  String JAXRS_GET = "jakarta.ws.rs.GET";
  /** JAXRS POST. */
  String JAXRS_POST = "jakarta.ws.rs.POST";
  /** JAXRS PUT. */
  String JAXRS_PUT = "jakarta.ws.rs.PUT";
  /** JAXRS DELETE. */
  String JAXRS_DELETE = "jakarta.ws.rs.DELETE";
  /** JAXRS PATCH. */
  String JAXRS_PATCH = "jakarta.ws.rs.PATCH";
  /** JAXRS HEAD. */
  String JAXRS_HEAD = "jakarta.ws.rs.HEAD";
  /** JAXRS OPTIONS. */
  String JAXRS_OPTIONS = "jakarta.ws.rs.OPTIONS";
  /** JAXRS Context. */
  String JAXRS_CONTEXT = "jakarta.ws.rs.core.Context";
  /** JAXRS Query Param. */
  String JAXRS_QUERY = "jakarta.ws.rs.QueryParam";
  /** JAXRS Path Param. */
  String JAXRS_PATH_PARAM = "jakarta.ws.rs.PathParam";
  /** JAXRS Cookie Param. */
  String JAXRS_COOKIE = "jakarta.ws.rs.CookieParam";
  /** JAXRS Header Param. */
  String JAXRS_HEADER = "jakarta.ws.rs.HeaderParam";
  /** JAXRS Form Param. */
  String JAXRS_FORM = "jakarta.ws.rs.FormParam";
  /** JAXRS PRODUCES. */
  String JAXRS_PRODUCES = "jakarta.ws.rs.Produces";
  /** JAXRS CONSUMES. */
  String JAXRS_CONSUMES = "jakarta.ws.rs.Consumes";
  /** JAXRS PATH. */
  String JAXRS_PATH = "jakarta.ws.rs.Path";

  /** HTTP method supported. */
  Set<String> HTTP_METHODS =
      Stream.of(
              GET,
              JAXRS_GET,
              POST,
              JAXRS_POST,
              PUT,
              JAXRS_PUT,
              DELETE,
              JAXRS_DELETE,
              PATCH,
              JAXRS_PATCH,
              HEAD,
              JAXRS_HEAD,
              OPTIONS,
              JAXRS_OPTIONS,
              CONNECT,
              TRACE)
          .collect(toUnmodifiableSet());

  /** Path parameters. */
  Set<String> PATH_PARAMS = Stream.of(PathParam, JAXRS_PATH_PARAM).collect(toUnmodifiableSet());

  /** Context params. */
  Set<String> CONTEXT_PARAMS = Stream.of(ContextParam, JAXRS_CONTEXT).collect(toUnmodifiableSet());

  /** Query parameters. */
  Set<String> QUERY_PARAMS = Stream.of(QueryParam, JAXRS_QUERY).collect(toUnmodifiableSet());

  /** Session parameters. */
  Set<String> SESSION_PARAMS = singleton(SessionParam);

  /** Cookie parameters. */
  Set<String> COOKIE_PARAMS = Stream.of(CookieParam, JAXRS_COOKIE).collect(toUnmodifiableSet());

  /** Header parameters. */
  Set<String> HEADER_PARAMS = Stream.of(HeaderParam, JAXRS_HEADER).collect(toUnmodifiableSet());

  /** Flash parameters. */
  Set<String> FLASH_PARAMS = Stream.of(FlashParam).collect(toUnmodifiableSet());

  /** Form parameters. */
  Set<String> FORM_PARAMS = Stream.of(FormParam, JAXRS_FORM).collect(toUnmodifiableSet());

  /** Parameter lookup. */
  Set<String> PARAM_LOOKUP = Stream.of(Param).collect(toUnmodifiableSet());

  /** Produces parameters. */
  Set<String> PRODUCES_PARAMS = Stream.of(Produces, JAXRS_PRODUCES).collect(toUnmodifiableSet());

  /** Consumes parameters. */
  Set<String> CONSUMES_PARAMS = Stream.of(Consumes, JAXRS_CONSUMES).collect(toUnmodifiableSet());

  /** Path parameters. */
  Set<String> PATH = Stream.of(Path, JAXRS_PATH).collect(toUnmodifiableSet());

  /**
   * Get an annotation value.
   *
   * @param mirror Annotation.
   * @param name Attribute name.
   * @return List of values.
   */
  static List<String> attribute(AnnotationMirror mirror, String name) {
    return attribute(mirror, name, v -> v.getValue().toString());
  }

  /**
   * Get an annotation value.
   *
   * @param mirror Annotation.
   * @param name Attribute name.
   * @param mapper Mapper function.
   * @param <T> Return type.
   * @return List of values.
   */
  static <T> List<T> attribute(
      AnnotationMirror mirror, String name, Function<AnnotationValue, T> mapper) {

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        mirror.getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().toString().equals(name)) {
        Object value = entry.getValue().getValue();
        if (value instanceof List) {
          List<AnnotationValue> values = (List<AnnotationValue>) value;
          return values.stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
        }
        T singleValue = mapper.apply(entry.getValue());
        return singleValue == null
            ? Collections.emptyList()
            : Collections.singletonList(singleValue);
      }
    }
    return Collections.emptyList();
  }
}
