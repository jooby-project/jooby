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

import io.jooby.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultMvcAnnotation implements MvcAnnotation {
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

  @Override public List<Class<? extends Annotation>> pathAnnotation() {
    return Collections.singletonList(Path.class);
  }

  @Override public String pathPattern(AnnotatedElement type) {
    Path path = type.getAnnotation(Path.class);
    return path == null ? null : path.value()[0];
  }

  @Override public boolean isQueryParam(Parameter parameter) {
    return parameter.getAnnotation(QueryParam.class) != null;
  }

  @Override public boolean isPathParam(Parameter parameter) {
    return parameter.getAnnotation(PathParam.class) != null;
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
}
