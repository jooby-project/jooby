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
  String DELETE = "io.jooby.annotation.DELETE";

  String GET = "io.jooby.annotation.GET";
  String HEAD = "io.jooby.annotation.HEAD";
  String OPTIONS = "io.jooby.annotation.OPTIONS";
  String PATCH = "io.jooby.annotation.PATCH";
  String POST = "io.jooby.annotation.POST";
  String PUT = "io.jooby.annotation.PUT";
  String Path = "io.jooby.annotation.Path";
  String Produces = "io.jooby.annotation.Produces";
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
