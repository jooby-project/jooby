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
package org.jooby.internal.swagger;

import static java.util.Objects.requireNonNull;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.MediaType;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.reflect.ReqParameterNameProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;

public class SwaggerBuilder {

  private static final Pattern VAR = Pattern.compile("\\:((?:[^/]+)+?)");

  private Set<Definition> routes;

  private ReqParameterNameProvider nameProvider;

  private Config config;

  private ObjectMapper mapper;

  @Inject
  public SwaggerBuilder(final Set<Route.Definition> routes,
      final ReqParameterNameProvider nameProvider,
      @Named("swagger") final Config config,
      @Named("swagger") final ObjectMapper mapper) {
    this.routes = requireNonNull(routes, "Routes are required.");
    this.nameProvider = requireNonNull(nameProvider, "NameProvider is required.");
    this.config = requireNonNull(config, "Swagger config is required.");
    this.mapper = requireNonNull(mapper, "Mapper is required.");
  }

  public <S extends Swagger> S build(final Predicate<String> predicate,
      final Class<S> swaggerType) {
    S swagger = newSwagger(swaggerType, mapper, config);

    Map<String, Path> paths = new LinkedHashMap<>();
    Set<String> tags = new LinkedHashSet<>();
    for (Route.Definition route : routes) {
      Route.Filter filter = route.filter();
      if (filter instanceof Route.MethodHandler) {
        String pattern = normalizePath(route.pattern());
        String tag = pattern.split("/")[1];
        if (!predicate.test(tag)) {
          continue;
        }
        Method method = ((Route.MethodHandler) filter).method();
        String[] pnames = nameProvider.names(method);

        /**
         * Tags
         */
        tags.add(tag);

        /**
         * Path
         */
        Path path = paths.get(pattern);
        if (path == null) {
          path = new Path();
          paths.put(pattern, path);
        }

        /**
         * Operation
         */
        Operation op = new Operation();
        op.addTag(tag);
        // consumes
        if (!route.consumes().containsAll(MediaType.ALL)) {
          route.consumes().forEach(type -> op.addConsumes(type.name()));
        }
        // produces
        if (!route.produces().containsAll(MediaType.ALL)) {
          route.produces().forEach(type -> op.addProduces(type.name()));
        }

        // parameter(s)
        java.lang.reflect.Parameter[] params = method.getParameters();
        ParameterBuilder pb = new ParameterBuilder();
        for (int i = 0; i < params.length; i++) {
          pb.build(route, pnames[i], params[i], swagger::addDefinition)
              .ifPresent(op::addParameter);
        }
        path.set(route.method().toLowerCase(), op);
      }
    }
    tags.forEach(t -> swagger.addTag(new Tag().name(t)));
    swagger.paths(paths);

    return swagger;
  }

  private <S extends Swagger> S newSwagger(final Class<S> type, final ObjectMapper mapper,
      final Config config) {
    // hack, get a hash from config and then use jackson to get the a swagger bean
    Map<String, Object> json = config.root().unwrapped();

    return mapper.convertValue(json, type);
  }

  private static String normalizePath(final String pattern) {
    Matcher matcher = VAR.matcher(pattern);
    StringBuilder result = new StringBuilder();
    int end = 0;
    while(matcher.find()) {
      result.append(pattern, end, matcher.start());
      result.append("{").append(matcher.group(1)).append("}");
      end = matcher.end();
    }
    result.append(pattern, end, pattern.length());
    return result.toString();
  }

}
