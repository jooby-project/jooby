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

import io.jooby.MediaType;

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
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JaxrsAnnotation implements MvcAnnotation {
  private static final List<Class<? extends Annotation>> M_ANN = Arrays
      .asList(GET.class,
          POST.class,
          PUT.class,
          DELETE.class,
          PATCH.class,
          HEAD.class,
          OPTIONS.class);

  private static final List<Class<? extends Annotation>> PARAM = Arrays
      .asList(PathParam.class,
          QueryParam.class,
          FormParam.class,
          HeaderParam.class);

  @Override public List<Class<? extends Annotation>> methodAnnotations() {
    return M_ANN;
  }

  @Override public String[] pathPattern(AnnotatedElement type) {
    Path path = type.getAnnotation(Path.class);
    return path == null ? null : new String[]{path.value()};
  }

  @Override public List<Class<? extends Annotation>> pathAnnotation() {
    return Collections.singletonList(Path.class);
  }

  @Override public boolean isPathParam(Parameter parameter) {
    return parameter.getAnnotation(PathParam.class) != null;
  }

  @Override public boolean isCookieParam(Parameter parameter) {
    return parameter.getAnnotation(CookieParam.class) != null;
  }

  @Override public boolean isQueryParam(Parameter parameter) {
    return parameter.getAnnotation(QueryParam.class) != null;
  }

  @Override public boolean isHeaderParam(Parameter parameter) {
    return parameter.getAnnotation(HeaderParam.class) != null;
  }

  @Override public boolean isFormParam(Parameter parameter) {
    return parameter.getAnnotation(FormParam.class) != null;
  }

  @Override public List<Class<? extends Annotation>> paramAnnotations() {
    return PARAM;
  }

  @Override public Set<MediaType> produces(Method method) {
    Produces produces = method.getAnnotation(Produces.class);
    return produces == null
        ? Collections.emptySet()
        : Stream.of(produces.value()).map(MediaType::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override public Set<MediaType> consumes(Method method) {
    Consumes consumes = method.getAnnotation(Consumes.class);
    return consumes == null
        ? Collections.emptySet()
        : Stream.of(consumes.value()).map(MediaType::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
