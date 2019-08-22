/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import io.jooby.SneakyThrows;
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
import io.jooby.annotations.QueryParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class JoobyAnnotationParser extends MvcAnnotationParserBase {

  private static final List<Class<? extends Annotation>> M_ANN = Arrays
      .asList(GET.class,
          POST.class,
          PUT.class,
          DELETE.class,
          PATCH.class,
          HEAD.class,
          OPTIONS.class);

  @Override protected List<Class<? extends Annotation>> httpMethods() {
    return M_ANN;
  }

  @Override protected MvcAnnotation create(Method method, Annotation annotation) {
    MvcAnnotation result = new MvcAnnotation(annotation.annotationType().getSimpleName(),
        path(method, annotation), produces(annotation),
        consumes(annotation), attributes(method));
    result.setCookieParam(CookieParam.class);
    result.setHeaderParam(HeaderParam.class);
    result.setPathParam(PathParam.class);
    result.setQueryParam(QueryParam.class);
    result.setFormParam(FormParam.class);
    result.setFlashParam(FlashParam.class);
    return result;
  }

  private String[] produces(Annotation httpMethod) {
    try {
      Method produces = httpMethod.getClass().getDeclaredMethod("produces");
      return (String[]) produces.invoke(httpMethod);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private String[] consumes(Annotation httpMethod) {
    try {
      Method consumes = httpMethod.getClass().getDeclaredMethod("consumes");
      return (String[]) consumes.invoke(httpMethod);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private String[] path(Method m, Annotation httpMethod) {
    return merge(pathPattern(m.getDeclaringClass(), null), pathPattern(m, httpMethod));
  }

  private String[] pathPattern(AnnotatedElement method, Annotation httpMethod) {
    String[] path = pathPatternFromMethod(httpMethod);
    if (path == null) {
      Path annotation = method.getAnnotation(Path.class);
      if (annotation != null) {
        path = annotation.value();
      }
    }
    return path;
  }

  private String[] pathPatternFromMethod(Annotation httpMethod) {
    if (httpMethod == null) {
      return null;
    }
    try {
      Method path = httpMethod.getClass().getDeclaredMethod("path");
      String[] result = (String[]) path.invoke(httpMethod);
      if (result.length == 0) {
        Method value = httpMethod.getClass().getDeclaredMethod("value");
        result = (String[]) value.invoke(httpMethod);
      }
      return result.length == 0 ? null : result;
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
