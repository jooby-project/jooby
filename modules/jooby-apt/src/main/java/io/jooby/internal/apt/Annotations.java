/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;

/**
 * Annotation constants used by the APT.
 *
 * @since 2.1.0
 */
public interface Annotations {

  String CONNECT = "io.jooby.annotation.CONNECT";
  String Consumes = "io.jooby.annotation.Consumes";
  String ContextParam = "io.jooby.annotation.ContextParam";
  String DELETE = "io.jooby.annotation.DELETE";
  String FlashParam = "io.jooby.annotation.FlashParam";
  String FormParam = "io.jooby.annotation.FormParam";
  String CookieParam = "io.jooby.annotation.CookieParam";
  String GET = "io.jooby.annotation.GET";
  String HEAD = "io.jooby.annotation.HEAD";
  String HeaderParam = "io.jooby.annotation.HeaderParam";
  String OPTIONS = "io.jooby.annotation.OPTIONS";
  String PATCH = "io.jooby.annotation.PATCH";
  String POST = "io.jooby.annotation.POST";
  String PUT = "io.jooby.annotation.PUT";
  String Param = "io.jooby.annotation.Param";
  String Path = "io.jooby.annotation.Path";
  String PathParam = "io.jooby.annotation.PathParam";
  String Produces = "io.jooby.annotation.Produces";
  String QueryParam = "io.jooby.annotation.QueryParam";
  String SessionParam = "io.jooby.annotation.SessionParam";
  String TRACE = "io.jooby.annotation.TRACE";

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
      Set.of(
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
          TRACE);

  /** Path parameters. */
  Set<String> PATH_PARAMS = Set.of(PathParam, JAXRS_PATH_PARAM);

  /** Context params. */
  Set<String> CONTEXT_PARAMS = Set.of(ContextParam, JAXRS_CONTEXT);

  /** Query parameters. */
  Set<String> QUERY_PARAMS = Set.of(QueryParam, JAXRS_QUERY);

  /** Session parameters. */
  Set<String> SESSION_PARAMS = Set.of(SessionParam);

  /** Cookie parameters. */
  Set<String> COOKIE_PARAMS = Set.of(CookieParam, JAXRS_COOKIE);

  /** Header parameters. */
  Set<String> HEADER_PARAMS = Set.of(HeaderParam, JAXRS_HEADER);

  /** Flash parameters. */
  Set<String> FLASH_PARAMS = Set.of(FlashParam);

  /** Form parameters. */
  Set<String> FORM_PARAMS = Set.of(FormParam, JAXRS_FORM);

  /** Parameter lookup. */
  Set<String> PARAM_LOOKUP = Set.of(Param);

  /** Produces parameters. */
  Set<String> PRODUCES_PARAMS = Set.of(Produces, JAXRS_PRODUCES);

  /** Consumes parameters. */
  Set<String> CONSUMES_PARAMS = Set.of(Consumes, JAXRS_CONSUMES);

  /** Path parameters. */
  Set<String> PATH = Set.of(Path, JAXRS_PATH);

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
    if (mirror != null) {
      for (var entry : mirror.getElementValues().entrySet()) {
        if (entry.getKey().getSimpleName().toString().equals(name)) {
          Object value = entry.getValue().getValue();
          if (value instanceof List) {
            List<AnnotationValue> values = (List<AnnotationValue>) value;
            return values.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
          }
          T singleValue = mapper.apply(entry.getValue());
          return singleValue == null
              ? Collections.emptyList()
              : Collections.singletonList(singleValue);
        }
      }
    }
    return Collections.emptyList();
  }
}
