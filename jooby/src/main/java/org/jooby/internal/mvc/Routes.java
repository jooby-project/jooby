/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.mvc;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jooby.Env;
import org.jooby.MediaType;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.internal.RouteMetadata;
import org.jooby.internal.reqparam.RequestParam;
import org.jooby.internal.reqparam.RequestParamNameProvider;
import org.jooby.internal.reqparam.RequestParamProvider;
import org.jooby.internal.reqparam.RequestParamProviderImpl;
import org.jooby.mvc.CONNECT;
import org.jooby.mvc.Consumes;
import org.jooby.mvc.DELETE;
import org.jooby.mvc.GET;
import org.jooby.mvc.HEAD;
import org.jooby.mvc.OPTIONS;
import org.jooby.mvc.PATCH;
import org.jooby.mvc.POST;
import org.jooby.mvc.PUT;
import org.jooby.mvc.Path;
import org.jooby.mvc.Produces;
import org.jooby.mvc.TRACE;

import com.google.common.collect.ImmutableSet;

public class Routes {

  @SuppressWarnings("unchecked")
  private static final Set<Class<? extends Annotation>> VERBS = ImmutableSet.of(GET.class,
      POST.class, PUT.class, DELETE.class, PATCH.class, HEAD.class, OPTIONS.class, TRACE.class,
      CONNECT.class);

  @SuppressWarnings({"unchecked", "rawtypes" })
  public static List<Route.Definition> routes(final Env env, final RouteMetadata classInfo,
      final Class<?> routeClass) {

    RequestParamProvider provider =
        new RequestParamProviderImpl(new RequestParamNameProvider(classInfo));

    String rootPath = path(routeClass);

    Map<Method, List<Class<?>>> methods = new HashMap<>();
    for (Method method : routeClass.getDeclaredMethods()) {
      List<Class<?>> annotations = new ArrayList<>();
      for (Class annotationType : VERBS) {
        Annotation annotation = method.getAnnotation(annotationType);
        if (annotation != null) {
          annotations.add(annotationType);
        }
      }
      if (annotations.size() > 0) {
        if (!Modifier.isPublic(method.getModifiers())) {
          throw new IllegalArgumentException("Not a public method: " + method);
        }
        methods.put(method, annotations);
      }
    }

    List<Definition> definitions = new ArrayList<>();

    methods
        .keySet()
        .stream()
        .sorted((m1, m2) -> {
          int l1 = classInfo.startAt(m1);
          int l2 = classInfo.startAt(m2);
          return l1 - l2;
        })
        .forEach(
            method -> {
              List<Class<?>> verbs = methods.get(method);
              String path = rootPath + "/" + path(method);
              String name = routeClass.getSimpleName() + "." + method.getName();
              List<MediaType> produces = produces(method);
              /**
               * Param provider: dev vs none dev
               */
              RequestParamProvider paramProvider = provider;
              if (!env.name().equals("dev")) {
                List<RequestParam> params = provider.parameters(method);
                paramProvider = (h) -> params;
              }

              for (Class<?> verb : verbs) {
                Definition definition = new Route.Definition(
                    verb.getSimpleName(), path, new MvcHandler(method, paramProvider, produces))
                    .produces(produces)
                    .consumes(consumes(method))
                    .name(name);

                definitions.add(definition);
              }
            });

    return definitions;
  }

  private static List<MediaType> produces(final Method method) {
    Function<AnnotatedElement, Optional<List<MediaType>>> fn = (element) -> {
      Produces produces = element.getAnnotation(Produces.class);
      if (produces != null) {
        return Optional.of(MediaType.valueOf(produces.value()));
      }
      return Optional.empty();
    };

    // method level
    return fn.apply(method)
        // class level
        .orElseGet(() -> fn.apply(method.getDeclaringClass())
            // none
            .orElse(MediaType.ALL));
  }

  private static List<MediaType> consumes(final Method method) {
    Function<AnnotatedElement, Optional<List<MediaType>>> fn = (element) -> {
      Consumes consumes = element.getAnnotation(Consumes.class);
      if (consumes != null) {
        return Optional.of(MediaType.valueOf(consumes.value()));
      }
      return Optional.empty();
    };

    // method level
    return fn.apply(method)
        // class level
        .orElseGet(() -> fn.apply(method.getDeclaringClass())
            // none
            .orElse(MediaType.ALL));
  }

  private static String path(final AnnotatedElement owner) {
    Path annotation = owner.getAnnotation(Path.class);
    if (annotation == null) {
      return "";
    }
    return annotation.value();
  }

}
