/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class MvcAnnotationParserBase implements MvcAnnotationParser {

  public final List<MvcAnnotation> parse(Method method) {
    List<MvcAnnotation> result = new ArrayList<>();

    for (Class<? extends Annotation> m : httpMethods()) {
      Annotation annotation = method.getAnnotation(m);
      if (annotation != null) {
        result.add(create(method, annotation));
      }
    }

    return result;
  }

  protected abstract MvcAnnotation create(Method method, Annotation annotation);

  protected abstract List<Class<? extends Annotation>> httpMethods();

  protected String[] merge(String[] parent, String[] path) {
    if (parent == null) {
      if (path == null) {
        return new String[]{"/"};
      }
      return path;
    }
    if (path == null) {
      return parent;
    }
    String[] result = new String[parent.length * path.length];
    int k = 0;

    for (String base : parent) {
      for (String element : path) {
        result[k] = base + "/" + element;
        k += 1;
      }
    }
    return result;
  }

  protected Map<Class<? extends Annotation>, Annotation> customAnnotations(Method method) {
    return Stream.of(method.getDeclaredAnnotations())
        .filter(annotation ->
            !"io.jooby.annotations".equals(annotation.annotationType().getPackage().getName())
                && !"javax.ws.rs".equals(annotation.annotationType().getPackage().getName()))
        .collect(Collectors.toMap(Annotation::annotationType, Function.identity()));
  }
}
