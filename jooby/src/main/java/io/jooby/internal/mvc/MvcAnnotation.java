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
import io.jooby.Throwing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MvcAnnotation {
  private String method;

  private String[] path;

  private List<MediaType> produces;

  private List<MediaType> consumes;

  private Class<? extends Annotation> headerParam;

  private Class<? extends Annotation> cookieParam;

  private Class<? extends Annotation> pathParam;

  private Class<? extends Annotation> queryParam;

  private Class<? extends Annotation> formParam;

  public MvcAnnotation(String method, String[] path, String[] produces, String[] consumes) {
    this.method = method;
    this.path = path;
    this.produces = types(produces);
    this.consumes = types(consumes);
  }

  private List<MediaType> types(String[] values) {
    if (values == null || values.length == 0) {
      return Collections.emptyList();
    }
    return Stream.of(values)
        .map(MediaType::valueOf)
        .collect(Collectors.toList());
  }

  public String getMethod() {
    return method;
  }

  public String[] getPath() {
    return path;
  }

  public List<MediaType> getProduces() {
    return produces;
  }

  public List<MediaType> getConsumes() {
    return consumes;
  }

  public boolean isPathParam(Parameter parameter) {
    return parameter.getAnnotation(pathParam) != null;
  }

  public boolean isQueryParam(Parameter parameter) {
    return parameter.getAnnotation(queryParam) != null;
  }

  public boolean isHeaderParam(Parameter parameter) {
    return parameter.getAnnotation(headerParam) != null;
  }

  public boolean isCookieParam(Parameter parameter) {
    return parameter.getAnnotation(cookieParam) != null;
  }

  public boolean isFormParam(Parameter parameter) {
    return parameter.getAnnotation(formParam) != null;
  }

  public void setHeaderParam(Class<? extends Annotation> headerParam) {
    this.headerParam = headerParam;
  }

  public void setCookieParam(Class<? extends Annotation> cookieParam) {
    this.cookieParam = cookieParam;
  }

  public void setPathParam(Class<? extends Annotation> pathParam) {
    this.pathParam = pathParam;
  }

  public void setQueryParam(Class<? extends Annotation> queryParam) {
    this.queryParam = queryParam;
  }

  public void setFormParam(Class<? extends Annotation> formParam) {
    this.formParam = formParam;
  }

  public String getName(Parameter parameter) {
    try {
      Class[] annotations = {headerParam, cookieParam, pathParam, queryParam, formParam};
      for (Class<? extends Annotation> annotationType : annotations) {
        Annotation annotation = parameter.getAnnotation(annotationType);
        if (annotation != null) {
          String name = ((String) annotationType.getDeclaredMethod("value").invoke(annotation))
              .trim();
          if (name.length() > 0) {
            return name;
          }
        }
      }
      return null;
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }
}
