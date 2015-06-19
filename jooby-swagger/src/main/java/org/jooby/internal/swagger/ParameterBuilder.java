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

import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.jooby.Route;
import org.jooby.mvc.Body;
import org.jooby.mvc.Header;

public class ParameterBuilder {

  public Optional<? extends Parameter> build(final Route.Definition route, final String name,
      final java.lang.reflect.Parameter parameter, final BiConsumer<String, Model> definitions) {
    ModelConverters converter = ModelConverters.getInstance();
    Type type = type(parameter);
    final Property property = converter.readAsProperty(type);
    if (property == null) {
      return Optional.empty();
    }
    boolean required = parameter.getType() != Optional.class;
    final Parameter result;
    if (parameter.getAnnotation(Body.class) != null) {
      BodyParameter bp = new BodyParameter();
      final Map<PropertyBuilder.PropertyId, Object> args =
          new EnumMap<PropertyBuilder.PropertyId, Object>(PropertyBuilder.PropertyId.class);
      bp.setSchema(PropertyBuilder.toModel(PropertyBuilder.merge(property, args)));
      for (Map.Entry<String, Model> entry : converter.readAll(type).entrySet()) {
        definitions.accept(entry.getKey(), entry.getValue());
      }
      result = bp;
    } else if (parameter.getAnnotation(Header.class) != null) {
      result = new HeaderParameter();
    } else {
      if (route.vars().contains(name)) {
        result = new PathParameter();
      } else {
        if (route.method().equals("GET")) {
          result = new QueryParameter();
        } else {
          result = new FormParameter();
        }
      }
    }
    // set type and format
    if (result instanceof SerializableParameter) {
      result.setDescription(property.getDescription());
      ((SerializableParameter) result).setType(property.getType());
      ((SerializableParameter) result).setFormat(property.getFormat());
    }

    result.setName(name);
    result.setRequired(required);

    return Optional.of(result);
  }

  private Type type(final java.lang.reflect.Parameter parameter) {
    if (parameter.getType() == Optional.class) {
      ParameterizedType pt = (ParameterizedType) parameter.getParameterizedType();
      return pt.getActualTypeArguments()[0];
    }
    return parameter.getParameterizedType();
  }
}
