/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import io.jooby.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  protected Map<String, Object> attributes(Method method) {
    Map<String, Object> attributes = new LinkedHashMap<>();

    List<Annotation> specificAttributes = Stream.of(method.getDeclaredAnnotations())
        .filter(annotation ->
            !"io.jooby.annotations".equals(annotation.annotationType().getPackage().getName())
                && !"javax.ws.rs".equals(annotation.annotationType().getPackage().getName()))
        .collect(Collectors.toList());

      specificAttributes.forEach(annotation -> attributes.putAll(extractAttributes(annotation)));

      return attributes;
  }

  protected Map<String, Object> extractAttributes(Annotation annotation) {
    Map<String, Object> annotationAttributes = new LinkedHashMap<>();

    Method[] attributesMethod = annotation.annotationType().getDeclaredMethods();
    for (Method attribute : attributesMethod) {
      try {
        Object value = attribute.invoke(annotation);
        annotationAttributes.put(attributeName(attribute, annotation), value);
      } catch (Exception x) {
        throw SneakyThrows.propagate(x);
      }
    }

    return annotationAttributes;
  }

  protected String attributeName(Method attr, Annotation annotation) {
    String name = attr.getName();
    String context = annotation.annotationType().getSimpleName();

    if (name.equals("value")) {
      return context;
    }

    return context + "." + name;
  }
}
