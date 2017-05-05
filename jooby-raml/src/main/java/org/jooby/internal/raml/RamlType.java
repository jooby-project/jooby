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
package org.jooby.internal.raml;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

public class RamlType {

  private String name;

  private String type;

  private boolean required = true;

  private boolean uniqueItems;

  private Map<String, RamlType> properties;

  private List<String> values;

  private String pattern;

  public RamlType(final String type) {
    this.type = type;
  }

  public String type() {
    return Optional.ofNullable(name).orElse(type);
  }

  public boolean required() {
    return required;
  }

  public Map<String, RamlType> properties() {
    return properties;
  }

  public String pattern(){ return pattern; }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof RamlType) {
      return type().equals(((RamlType) obj).type());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public String toString() {
    return toString(0);
  }

  private String typeRef(final int level) {
    StringBuilder buff = new StringBuilder();
    buff.append(indent(level)).append("type: ").append(type()).append("\n");
    if (!required) {
      buff.append(indent(level)).append("required: ").append(required).append("\n");
    }
    return buff.toString();
  }

  public String toString(int level) {
    StringBuilder buff = new StringBuilder();
    if (name != null) {
      buff.append(indent(level)).append(name).append(":\n");
      level += 2;
    }
    buff.append(indent(level)).append("type: ").append(type).append("\n");
    if (!required) {
      buff.append(indent(level)).append("required: ").append(required).append("\n");
    }
    if (uniqueItems) {
      buff.append(indent(level)).append("uniqueItems: ").append(uniqueItems).append("\n");
    }
    if (properties != null) {
      buff.append(indent(level)).append("properties:\n");
      int l = level + 2;
      properties.forEach((n, t) -> {
        buff.append(indent(l)).append(n).append(":\n").append(t.typeRef(l + 2));
      });
    }
    if (values != null) {
      buff.append(indent(level)).append("enum: ").append(values.toString()).append("\n");
    }
    if (pattern != null) {
      buff.append(indent(level)).append("pattern: ").append(pattern).append("\n");
    }
    buff.setLength(buff.length() - 1);
    return buff.toString();
  }

  private String indent(final int level) {
    StringBuilder buff = new StringBuilder();
    for (int i = 0; i < level; i++) {
      buff.append(" ");
    }
    return buff.toString();
  }

  public static RamlType parse(final Type type) {
    return parse(type, new HashMap<>());
  }

  public static List<RamlType> parseAll(final Type type) {
    Map<Type, RamlType> ctx = new HashMap<>();
    parse(type, ctx);
    return ImmutableList.copyOf(ctx.values());
  }

  private static RamlType parse(final Type type, final Map<Type, RamlType> ctx) {
    RamlType ramlType = ctx.get(type);
    if (ramlType == null) {
      ramlType = simpleParse(type);
      ctx.put(type, ramlType);
      if (ramlType.type.equals("array")) {
        RamlType items = parse(componentType(type), ctx);
        ramlType.type = items.type() + "[]";
      } else if (ramlType.type.equals("object")) {
        Class<?> rawType = toClass(type);
        Field[] fields = rawType.getDeclaredFields();
        Map<String, RamlType> props = new LinkedHashMap<>();
        for (Field field : fields) {
          if(!field.getName().startsWith("_")) { // only not hidden properties
            RamlType ftype = parse(field.getGenericType(), ctx);
            if (field.getType().isArray()) {
              String ctype = ramlTypeName(field.getType());
              ftype.type = (ctype == null ? ftype.type() : ctype) + "[]";
              ftype.name = null;
              ftype.properties = null;
            }
            props.put(field.getName(), ftype);
          }
        }
        ramlType.properties = props;
      }
    }
    return ramlType;
  }

  @SuppressWarnings("rawtypes")
  private static RamlType simpleParse(final Type type) {
    if (type == null) {
      return new RamlType("object");
    }
    Class<?> rawType = toClass(type);
    Class<?> componentType = componentType(type);
    boolean optional = rawType == Optional.class;
    if (optional && componentType != null) {
      rawType = componentType;
    }
    String ramlName = ramlTypeName(rawType);
    if (ramlName != null) {
      return new RamlType(ramlName).required(!optional);
    }

    RamlType complex;
    if (Collection.class.isAssignableFrom(rawType) || rawType.isArray()) {
      complex = new RamlType("array");
      complex.uniqueItems = Set.class.isAssignableFrom(rawType);
    } else if (rawType.isEnum()) {
      complex = new RamlType("string");
      complex.name = rawType.getSimpleName();
      Object[] values = rawType.getEnumConstants();
      List<String> enums = new ArrayList<>();
      for (Object value : values) {
        enums.add(((Enum) value).name());
      }
      complex.values = enums;
    } else if(UUID.class.isAssignableFrom(rawType)){
      complex = new RamlType("string");
      complex.name = "uuid";
      complex.pattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    } else {
      complex = new RamlType("object");
      complex.name = rawType.getSimpleName();
    }
    return complex;
  }

  private static String ramlTypeName(final Class<?> rawType) {
    String typeName = rawType.getTypeName();
    switch (typeName) {
      case "byte":
      case "java.lang.Byte":
      case "short":
      case "java.lang.Short":
      case "int":
      case "java.lang.Integer":
      case "long":
      case "java.lang.Long":
        return "integer";
      case "float":
      case "java.lang.Float":
      case "double":
      case "java.lang.Double":
        return "number";
      case "boolean":
      case "java.lang.Boolean":
        return "boolean";
      case "char":
      case "java.lang.Character":
      case "java.lang.String":
        return "string";
      case "org.jooby.Upload":
        return "file";
      case "java.util.Date":
      case "java.time.LocalDate":
        return "date";
    }
    return null;
  }

  private RamlType required(final boolean required) {
    this.required = required;
    return this;
  }

  @SuppressWarnings("rawtypes")
  private static Class<?> toClass(final Object type) {
    if (type == null) {
      return Object.class;
    }
    if (type instanceof ParameterizedType) {
      return toClass(((ParameterizedType) type).getRawType());
    }
    if (type instanceof WildcardType) {
      WildcardType wtype = ((WildcardType) type);
      Type[] lowerBounds = wtype.getLowerBounds();
      Type[] upperBounds = wtype.getUpperBounds();
      if (lowerBounds.length == 0) {
        return toClass(upperBounds[0]);
      } else {
        return toClass(lowerBounds[0]);
      }
    }
    if (type instanceof TypeVariable) {
      TypeVariable tvar = (TypeVariable) type;
      return toClass(tvar.getBounds()[0]);
    }
    if (type instanceof GenericArrayType) {
      GenericArrayType array = (GenericArrayType) type;
      return toClass(array.getGenericComponentType());
    }
    return (Class<?>) type;
  }

  @SuppressWarnings("rawtypes")
  private static Class<?> componentType(final Type type) {
    if (type instanceof ParameterizedType) {
      return toClass(((ParameterizedType) type).getActualTypeArguments()[0]);
    } else if (type instanceof Class) {
      if (((Class) type).isArray()) {
        return ((Class) type).getComponentType();
      }
    }
    return null;
  }

  public boolean isCustom() {
    return properties != null ||
            values != null ||
            pattern != null;
  }

  public boolean isObject() {
    return properties != null;
  }

  public boolean isEnum() {
    return values != null;
  }

}
