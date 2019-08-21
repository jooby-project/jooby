/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class JaxrsAnnotationParser extends MvcAnnotationParserBase {

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
        path(method),
        produces(method), consumes(method), customAnnotations(method));
    result.setCookieParam(CookieParam.class);
    result.setHeaderParam(HeaderParam.class);
    result.setPathParam(PathParam.class);
    result.setQueryParam(QueryParam.class);
    result.setFormParam(FormParam.class);
    return result;
  }

  private String[] produces(Method method) {
    Produces produces = method.getAnnotation(Produces.class);
    return produces == null ? null : produces.value();
  }

  private String[] consumes(Method method) {
    Consumes consumes = method.getAnnotation(Consumes.class);
    return consumes == null ? null : consumes.value();
  }

  private String[] path(Method m) {
    return merge(pathPattern(m.getDeclaringClass()), pathPattern(m));
  }

  private String[] pathPattern(AnnotatedElement element) {
    Path path = element.getAnnotation(Path.class);
    return path == null ? null : new String[]{path.value()};
  }
}
