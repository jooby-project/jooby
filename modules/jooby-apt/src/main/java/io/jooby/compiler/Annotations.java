/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.compiler;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
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
  String PATH = "io.jooby.annotations.Path";

  String GET = "io.jooby.annotations.GET";

  String POST = "io.jooby.annotations.POST";

  Set<String> HTTP_METHODS = unmodifiableSet(new LinkedHashSet<>(asList(GET, POST)));

  String PATH_PARAM = "io.jooby.annotations.PathParam";

  String QUERY_PARAM = "io.jooby.annotations.QueryParam";

  String COOKIE_PARAM = "io.jooby.annotations.CookieParam";

  String HEADER_PARAM = "io.jooby.annotations.HeaderParam";

  String FLASH_PARAM = "io.jooby.annotations.FlashParam";

  String FORM_PARAM = "io.jooby.annotations.FormParam";

  String PRODUCES_PARAM = "io.jooby.annotations.Produces";

  String CONSUMES_PARAM = "io.jooby.annotations.Consumes";

  Set<String> PATH_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(PATH_PARAM)));

  Set<String> QUERY_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(QUERY_PARAM)));

  Set<String> COOKIE_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(COOKIE_PARAM)));

  Set<String> HEADER_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(HEADER_PARAM)));

  Set<String> FLASH_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(FLASH_PARAM)));

  Set<String> FORM_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(FORM_PARAM)));

  Set<String> PRODUCES_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(PRODUCES_PARAM)));

  Set<String> CONSUMES_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(CONSUMES_PARAM)));

  static List<String> attribute(AnnotationMirror mirror, String name) {
    Function<Object, String> cleanValue = arg -> ((AnnotationValue) arg).getValue().toString();

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
