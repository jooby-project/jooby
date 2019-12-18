/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.BeanConverter;
import io.jooby.Router;
import io.jooby.ValueNode;
import io.jooby.internal.converter.BigDecimalConverter;
import io.jooby.internal.converter.BigIntegerConverter;
import io.jooby.internal.converter.CharsetConverter;
import io.jooby.internal.converter.DateConverter;
import io.jooby.internal.converter.DurationConverter;
import io.jooby.internal.converter.InstantConverter;
import io.jooby.internal.converter.LocalDateConverter;
import io.jooby.internal.converter.LocalDateTimeConverter;
import io.jooby.internal.converter.PeriodConverter;
import io.jooby.internal.converter.ReflectiveBeanConverter;
import io.jooby.internal.converter.StatusCodeConverter;
import io.jooby.internal.converter.TimeZoneConverter;
import io.jooby.internal.converter.URIConverter;
import io.jooby.internal.converter.UUIDConverter;
import io.jooby.internal.converter.ValueOfConverter;
import io.jooby.internal.converter.ZoneIdConverter;
import io.jooby.internal.reflect.$Types;
import io.jooby.ValueConverter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ValueConverters {

  static List<ValueConverter> defaultConverters() {
    List<ValueConverter> result = new ArrayList<>();
    result.add(new UUIDConverter());

    result.add(new InstantConverter());
    result.add(new DateConverter());
    result.add(new LocalDateConverter());
    result.add(new LocalDateTimeConverter());

    result.add(new BigDecimalConverter());
    result.add(new BigIntegerConverter());

    result.add(new DurationConverter());
    result.add(new PeriodConverter());

    result.add(new CharsetConverter());

    result.add(new StatusCodeConverter());

    result.add(new TimeZoneConverter());
    result.add(new ZoneIdConverter());

    result.add(new URIConverter());

    return result;
  }

  static void addFallbackConverters(List<ValueConverter> converters) {
    converters.add(new ValueOfConverter());
  }

  static void addFallbackBeanConverters(List<BeanConverter> converters) {
    converters.add(new ReflectiveBeanConverter());
  }

  public static <T> T convert(ValueNode value, Type type, Router router) {
    Class rawType = $Types.getRawType(type);
    if (List.class.isAssignableFrom(rawType)) {
      return (T) Collections
          .singletonList(convert(value, $Types.parameterizedType0(type), router));
    }
    if (Set.class.isAssignableFrom(rawType)) {
      return (T) Collections.singleton(convert(value, $Types.parameterizedType0(type), router));
    }
    if (Optional.class.isAssignableFrom(rawType)) {
      return (T) Optional.ofNullable(convert(value, $Types.parameterizedType0(type), router));
    }
    return convert(value, rawType, router);
  }

  public static <T> T convert(ValueNode value, Class type, Router router) {
    if (type == String.class) {
      return (T) value.valueOrNull();
    }
    if (type == int.class) {
      return (T) Integer.valueOf(value.intValue());
    }
    if (type == long.class) {
      return (T) Long.valueOf(value.longValue());
    }
    if (type == float.class) {
      return (T) Float.valueOf(value.floatValue());
    }
    if (type == double.class) {
      return (T) Double.valueOf(value.doubleValue());
    }
    if (type == boolean.class) {
      return (T) Boolean.valueOf(value.booleanValue());
    }
    if (type == byte.class) {
      return (T) Byte.valueOf(value.byteValue());
    }
    if (type.isEnum()) {
      return (T) enumValue(value, type);
    }
    // Wrapper
    if (type == Integer.class) {
      return (T) (value.isMissing() ? null : Integer.valueOf(value.intValue()));
    }
    if (type == Long.class) {
      return (T) (value.isMissing() ? null : Long.valueOf(value.longValue()));
    }
    if (type == Float.class) {
      return (T) (value.isMissing() ? null : Float.valueOf(value.floatValue()));
    }
    if (type == Double.class) {
      return (T) (value.isMissing() ? null : Double.valueOf(value.doubleValue()));
    }
    if (type == Byte.class) {
      return (T) (value.isMissing() ? null : Byte.valueOf(value.byteValue()));
    }

    if (value.isSingle()) {
      for (ValueConverter converter : router.getConverters()) {
        if (converter.supports(type)) {
          return (T) converter.convert(value, type);
        }
      }
    } else if (value.isObject()) {
      for (BeanConverter converter : router.getBeanConverters()) {
        if (converter.supports(type)) {
          return (T) converter.convert(value, type);
        }
      }
    }
    return null;
  }

  private static Object enumValue(ValueNode value, Class type) {
    try {
      return Enum.valueOf(type, value.value().toUpperCase());
    } catch (IllegalArgumentException x) {
      String name = value.value();
      // Fallback: Ignore case:
      Set<Enum> enums = EnumSet.allOf(type);
      for (Enum e : enums) {
        if (e.name().equalsIgnoreCase(name)) {
          return e;
        }
      }
      throw x;
    }
  }
}
