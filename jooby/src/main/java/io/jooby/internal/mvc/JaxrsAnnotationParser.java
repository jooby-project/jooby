/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
        produces(method), consumes(method));
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
