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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.spec.RouteParam;
import org.jooby.spec.RouteProcessor;
import org.jooby.spec.RouteResponse;
import org.jooby.spec.RouteSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;

import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;

public class SwaggerBuilder {

  private static final Pattern VAR = Pattern.compile("\\:((?:[^/]+)+?)");

  private static final Pattern SENTENCE = Pattern.compile("\\.|\\n");

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private List<Route.Definition> routes;

  private Config config;

  private ObjectMapper mapper;

  private Class<? extends Jooby> appClass;

  @Inject
  public SwaggerBuilder(@Named("application.class") final Class<? extends Jooby> appClass,
      final Set<Route.Definition> routes,
      @Named("swagger") final Config config,
      @Named("swagger") final ObjectMapper mapper) {
    requireNonNull(appClass, "App class is required.");
    requireNonNull(routes, "Routes are required.");
    this.routes = ImmutableList.copyOf(routes);
    this.appClass = appClass;
    this.config = requireNonNull(config, "Swagger config is required.");
    this.mapper = requireNonNull(mapper, "Mapper is required.");
  }

  public <S extends Swagger> S build(final Optional<String> tagFilter,
      final Predicate<RouteSpec> filter, final Function<RouteSpec, String> tagProvider,
      final Class<S> swaggerType) {
    S swagger = newSwagger(swaggerType, mapper, config);

    RouteProcessor processor = new RouteProcessor();
    List<RouteSpec> specs = processor.process(appClass, routes);
    Map<String, Path> paths = new LinkedHashMap<>();
    Map<String, Tag> tags = new HashMap<>();
    for (RouteSpec route : specs) {
      String tagname = tagProvider.apply(route);
      boolean process = filter.test(route) && tagFilter.map(tagname::equals).orElse(true);
      if (process) {
        String pattern = normalizePath(route.pattern());

        /**
         * Tags
         */
        Tag tag = tags.get(tagname);
        if (tag == null) {
          tag = new Tag();
          tag.name(tagname);
          route.summary().ifPresent(tag::description);
          tags.put(tagname, tag);
          swagger.addTag(tag);
        }

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
        op.addTag(tag.getName());

        /**
         * Doc and summary
         */
        route.doc().ifPresent(doc -> {
          String summary = Splitter.on(SENTENCE)
              .trimResults()
              .omitEmptyStrings()
              .split(doc)
              .iterator()
              .next();

          op.summary(summary);
          op.description(doc);
        });

        /** Consumes/Produces . */
        route.consumes().stream()
            .filter(t -> t.equals("*/*"))
            .forEach(type -> op.addConsumes(type));
        route.produces().stream()
            .filter(t -> t.equals("*/*"))
            .forEach(type -> op.addProduces(type));

        /**
         * Params
         */
        List<RouteParam> params = route.params();
        for (RouteParam param : params) {
          op.addParameter(param(param, swagger::addDefinition));
        }

        /**
         * Response
         */
        Response rsp = new Response();
        RouteResponse routersp = route.response();
        Map<Integer, String> statusCodes = Maps.newHashMap(routersp.statusCodes());
        int statusCode = routersp.statusCode();
        String doc = routersp.doc().orElse(statusCodes.get(statusCode));
        rsp.description(doc);
        rsp.schema(ModelConverters.getInstance().readAsProperty(routersp.type()));
        op.addResponse(String.valueOf(statusCode), rsp);
        // additional status codes
        statusCodes.forEach((sc, label) -> {
          if (statusCode != sc.intValue()) {
            op.addResponse(sc.toString(), new Response().description(label));
          }
        });

        path.set(route.method().toLowerCase(), op);
      } else {
        log.debug("skipping: {}", route);
      }
    }
    swagger.paths(paths);

    return swagger;
  }

  @SuppressWarnings("rawtypes")
  private Parameter param(final RouteParam param, final BiConsumer<String, Model> definitions) {
    ModelConverters converter = ModelConverters.getInstance();
    Type type = paramType(param.type());
    final Property property = converter.readAsProperty(type);

    boolean required = !param.optional();
    final Parameter result;
    switch (param.paramType()) {
      case BODY: {
        BodyParameter bp = new BodyParameter();
        final Map<PropertyBuilder.PropertyId, Object> args = new EnumMap<PropertyBuilder.PropertyId, Object>(
            PropertyBuilder.PropertyId.class);
        bp.setSchema(PropertyBuilder.toModel(PropertyBuilder.merge(property, args)));
        for (Map.Entry<String, Model> entry : converter.readAll(type).entrySet()) {
          definitions.accept(entry.getKey(), entry.getValue());
        }
        result = bp;
      }
        break;
      case PATH: {
        result = new PathParameter();
      }
        break;
      case FORM: {
        result = new FormParameter();
      }
        break;
      default: {
        result = new QueryParameter();
      }
        break;
    }

    // set type, format and items
    result.setDescription(property.getDescription());
    serializable(result).ifPresent(ser -> {
      ser.setDescription(property.getDescription());
      ser.setType(property.getType());
      ser.setFormat(property.getFormat());
      if (property instanceof ArrayProperty) {
        ser.setItems(((ArrayProperty) property).getItems());
      }
      if (type instanceof Class) {
        Class possibleEnum = (Class) type;
        Object[] values = possibleEnum.getEnumConstants();
        if (values != null) {
          List<String> enums = new ArrayList<>();
          for (Object value : values) {
            enums.add(((Enum) value).name());
          }
          ser.setEnum(enums);
        }
      }
    });

    result.setName(param.name());
    result.setRequired(required);
    param.doc().ifPresent(result::setDescription);
    return result;
  }

  private Optional<SerializableParameter> serializable(final Parameter param) {
    if (param instanceof SerializableParameter) {
      return Optional.of((SerializableParameter) param);
    }
    return Optional.empty();
  }

  private Type paramType(final Type type) {
    if (type == Optional.class && type instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) type;
      return pt.getActualTypeArguments()[0];
    }
    return type;
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
    while (matcher.find()) {
      result.append(pattern, end, matcher.start());
      result.append("{").append(matcher.group(1)).append("}");
      end = matcher.end();
    }
    result.append(pattern, end, pattern.length());
    return result.toString();
  }

}
