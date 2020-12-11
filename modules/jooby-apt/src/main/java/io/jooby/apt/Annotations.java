/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

import io.jooby.annotations.CONNECT;
import io.jooby.annotations.Consumes;
import io.jooby.annotations.ContextParam;
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
import io.jooby.annotations.Param;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.jooby.annotations.Produces;
import io.jooby.annotations.QueryParam;
import io.jooby.annotations.SessionParam;
import io.jooby.annotations.TRACE;

/**
 * Annotation constants used by the APT.
 *
 * @since 2.1.0
 */
public interface Annotations {
  /** JAXRS GET. */
  String JAXRS_GET = "javax.ws.rs.GET";
  /** JAXRS POST. */
  String JAXRS_POST = "javax.ws.rs.POST";
  /** JAXRS PUT. */
  String JAXRS_PUT = "javax.ws.rs.PUT";
  /** JAXRS DELETE. */
  String JAXRS_DELETE = "javax.ws.rs.DELETE";
  /** JAXRS PATCH. */
  String JAXRS_PATCH = "javax.ws.rs.PATCH";
  /** JAXRS HEAD. */
  String JAXRS_HEAD = "javax.ws.rs.HEAD";
  /** JAXRS OPTIONS. */
  String JAXRS_OPTIONS = "javax.ws.rs.OPTIONS";
  /** JAXRS Context. */
  String JAXRS_CONTEXT = "javax.ws.rs.core.Context";
  /** JAXRS Query Param. */
  String JAXRS_QUERY = "javax.ws.rs.QueryParam";
  /** JAXRS Cookie Param. */
  String JAXRS_COOKIE = "javax.ws.rs.CookieParam";
  /** JAXRS Header Param. */
  String JAXRS_HEADER = "javax.ws.rs.HeaderParam";
  /** JAXRS Form Param. */
  String JAXRS_FORM = "javax.ws.rs.FormParam";
  /** JAXRS PRODUCES. */
  String JAXRS_PRODUCES = "javax.ws.rs.Produces";
  /** JAXRS CONSUMES. */
  String JAXRS_CONSUMES = "javax.ws.rs.Consumes";
  /** JAXRS PATH. */
  String JAXRS_PATH = "javax.ws.rs.Path";

  /**
   * HTTP method supported.
   */
  Set<String> HTTP_METHODS = unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
      GET.class.getName(), JAXRS_GET,

      POST.class.getName(), JAXRS_POST,

      PUT.class.getName(), JAXRS_PUT,

      DELETE.class.getName(), JAXRS_DELETE,

      PATCH.class.getName(), JAXRS_PATCH,

      HEAD.class.getName(), JAXRS_HEAD,

      OPTIONS.class.getName(), JAXRS_OPTIONS,

      CONNECT.class.getName(),

      TRACE.class.getName()
  )));

  /**
   * Path parameters.
   */
  Set<String> PATH_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(PathParam.class.getName())));

  /** Context params. */
  Set<String> CONTEXT_PARAMS = unmodifiableSet(
      new LinkedHashSet<>(asList(ContextParam.class.getName(), JAXRS_CONTEXT)));

  /**
   * Query parameters.
   */
  Set<String> QUERY_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      QueryParam.class.getName(), JAXRS_QUERY
  )));

  /**
   * Session parameters.
   */
  Set<String> SESSION_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      SessionParam.class.getName()
  )));

  /**
   * Cookie parameters.
   */
  Set<String> COOKIE_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      CookieParam.class.getName(), JAXRS_COOKIE
  )));

  /**
   * Header parameters.
   */
  Set<String> HEADER_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      HeaderParam.class.getName(), JAXRS_HEADER
  )));

  /**
   * Flash parameters.
   */
  Set<String> FLASH_PARAMS = unmodifiableSet(
      new LinkedHashSet<>(asList(FlashParam.class.getName())));

  /**
   * Form parameters.
   */
  Set<String> FORM_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      FormParam.class.getName(), JAXRS_FORM
  )));

  /**
   * Parameter lookup.
   */
  Set<String> PARAM_LOOKUP = unmodifiableSet(Collections.singleton(
      Param.class.getName()
  ));

  /**
   * Produces parameters.
   */
  Set<String> PRODUCES_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      Produces.class.getName(), JAXRS_PRODUCES
  )));

  /**
   * Consumes parameters.
   */
  Set<String> CONSUMES_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(
      Consumes.class.getName(), JAXRS_CONSUMES
  )));

  /**
   * Path parameters.
   */
  Set<String> PATH = unmodifiableSet(new LinkedHashSet<>(asList(
      Path.class.getName(), JAXRS_PATH
  )));

  /**
   * Get an annotation value.
   *
   * @param mirror Annotation.
   * @param name Attribute name.
   * @return List of values.
   */
  static @Nonnull List<String> attribute(@Nonnull AnnotationMirror mirror, @Nonnull String name) {
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
  static @Nonnull <T> List<T> attribute(@Nonnull AnnotationMirror mirror,
      @Nonnull String name, @Nonnull Function<AnnotationValue, T> mapper) {

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
        .getElementValues().entrySet()) {
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
    return Collections.emptyList();
  }
}
