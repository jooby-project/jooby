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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface MvcAnnotation {

  List<Class<? extends Annotation>> methodAnnotations();

  List<Class<? extends Annotation>> pathAnnotation();

  List<Class<? extends Annotation>> paramAnnotations();

  boolean isPathParam(Parameter parameter);

  boolean isQueryParam(Parameter parameter);

  boolean isHeaderParam(Parameter parameter);

  boolean isCookieParam(Parameter parameter);

  boolean isFormParam(Parameter parameter);

  String pathPattern(AnnotatedElement type);

  default String paramName(AnnotatedElement type) {
    try {
      for (Class<? extends Annotation> annotationType : paramAnnotations()) {
        Annotation annotation = type.getAnnotation(annotationType);
        if (annotation != null) {
          String name = ((String) annotationType.getDeclaredMethod("value").invoke(annotation)).trim();
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

  Set<MediaType> produces(Method method);

  Set<MediaType> consumes(Method method);

  default List<String> httpMethod(Method method) {
    List<String> result = new ArrayList<>();
    for (Class<? extends Annotation> m : methodAnnotations()) {
      Annotation annotation = method.getAnnotation(m);
      if (annotation != null) {
        result.add(annotation.annotationType().getSimpleName());
      }
    }
    if (result.size() == 0) {
      for (Class<? extends Annotation> pathAnnotation : pathAnnotation()) {
        if (method.getAnnotation(pathAnnotation) != null) {
          result.add("GET");
        }
      }
    }
    return result;
  }

  static MvcAnnotation create(ClassLoader loader) {
    try {
      loader.loadClass("javax.ws.rs.GET");
      JaxrsAnnotation jaxrs = new JaxrsAnnotation();
      DefaultMvcAnnotation def = new DefaultMvcAnnotation();
      List<Class<? extends Annotation>> methodAnnotations = new ArrayList<>();
      methodAnnotations.addAll(jaxrs.methodAnnotations());
      methodAnnotations.addAll(def.methodAnnotations());

      List<Class<? extends Annotation>> pathAnnotation = new ArrayList<>();
      pathAnnotation.addAll(jaxrs.pathAnnotation());
      pathAnnotation.addAll(def.pathAnnotation());

      List<Class<? extends Annotation>> paramAnnotations = new ArrayList<>();
      paramAnnotations.addAll(jaxrs.paramAnnotations());
      paramAnnotations.addAll(def.paramAnnotations());

      return new MvcAnnotation() {
        @Override public List<Class<? extends Annotation>> methodAnnotations() {
          return methodAnnotations;
        }

        @Override public String pathPattern(AnnotatedElement type) {
          String path = jaxrs.pathPattern(type);
          return path == null ? def.pathPattern(type) : path;
        }

        @Override public List<Class<? extends Annotation>> pathAnnotation() {
          return pathAnnotation;
        }

        @Override public List<Class<? extends Annotation>> paramAnnotations() {
          return paramAnnotations;
        }

        @Override public boolean isPathParam(Parameter parameter) {
          return jaxrs.isPathParam(parameter) || def.isPathParam(parameter);
        }

        @Override public boolean isCookieParam(Parameter parameter) {
          return jaxrs.isCookieParam(parameter) || def.isCookieParam(parameter);
        }

        @Override public boolean isQueryParam(Parameter parameter) {
          return jaxrs.isQueryParam(parameter) || def.isQueryParam(parameter);
        }

        @Override public boolean isHeaderParam(Parameter parameter) {
          return jaxrs.isHeaderParam(parameter) || def.isHeaderParam(parameter);
        }

        @Override public boolean isFormParam(Parameter parameter) {
          return jaxrs.isFormParam(parameter) || def.isFormParam(parameter);
        }

        @Override public Set<MediaType> produces(Method method) {
          Set<MediaType> result = new LinkedHashSet<>();
          result.addAll(jaxrs.produces(method));
          result.addAll(def.produces(method));
          return result;
        }

        @Override public Set<MediaType> consumes(Method method) {
          Set<MediaType> result = new LinkedHashSet<>();
          result.addAll(jaxrs.consumes(method));
          result.addAll(def.consumes(method));
          return result;
        }
      };
    } catch (ClassNotFoundException x) {
      return new DefaultMvcAnnotation();
    }
  }

}
