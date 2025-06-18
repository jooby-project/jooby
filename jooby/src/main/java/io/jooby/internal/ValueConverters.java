/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.lang.reflect.Type;
import java.util.*;

import io.jooby.ValueConverter;
import io.jooby.ValueNode;
import io.jooby.internal.converter.BuiltinConverter;
import io.jooby.internal.converter.StringConstructorConverter;
import io.jooby.internal.converter.ValueOfConverter;
import io.jooby.value.ValueFactory;

public class ValueConverters {

  public static List<ValueConverter<?>> defaultConverters() {
    var converters = new ArrayList<ValueConverter<?>>(List.of(BuiltinConverter.values()));
    converters.add(new ValueOfConverter());
    converters.add(new StringConstructorConverter());
    return converters;
  }

  public static <T> T convert(ValueNode value, Type type, ValueFactory router) {
    //    Class rawType = $Types.getRawType(type);
    //    if (List.class.isAssignableFrom(rawType)) {
    //      return (T) Collections.singletonList(convert(value, $Types.parameterizedType0(type),
    // router));
    //    }
    //    if (Set.class.isAssignableFrom(rawType)) {
    //      return (T) Collections.singleton(convert(value, $Types.parameterizedType0(type),
    // router));
    //    }
    //    if (Optional.class.isAssignableFrom(rawType)) {
    //      return (T) Optional.ofNullable(convert(value, $Types.parameterizedType0(type), router));
    //    }
    return convert(value, type, router, false);
    //    return (T) router.convert(type, value);
  }

  public static <T> T convert(
      ValueNode value, Type type, ValueFactory router, boolean allowEmptyBean) {
    return (T) router.convert(type, value);
    //    if (type == String.class) {
    //      return (T) value.valueOrNull();
    //    }
    //    if (type == int.class) {
    //      return (T) Integer.valueOf(value.intValue());
    //    }
    //    if (type == long.class) {
    //      return (T) Long.valueOf(value.longValue());
    //    }
    //    if (type == float.class) {
    //      return (T) Float.valueOf(value.floatValue());
    //    }
    //    if (type == double.class) {
    //      return (T) Double.valueOf(value.doubleValue());
    //    }
    //    if (type == boolean.class) {
    //      return (T) Boolean.valueOf(value.booleanValue());
    //    }
    //    if (type == byte.class) {
    //      return (T) Byte.valueOf(value.byteValue());
    //    }
    //    if (Enum.class.isAssignableFrom(type)) {
    //      return (T) enumValue(value, type);
    //    }
    //    // Wrapper
    //    if (type == Integer.class) {
    //      return (T) (value.isMissing() ? null : Integer.valueOf(value.intValue()));
    //    }
    //    if (type == Long.class) {
    //      return (T) (value.isMissing() ? null : Long.valueOf(value.longValue()));
    //    }
    //    if (type == Float.class) {
    //      return (T) (value.isMissing() ? null : Float.valueOf(value.floatValue()));
    //    }
    //    if (type == Double.class) {
    //      return (T) (value.isMissing() ? null : Double.valueOf(value.doubleValue()));
    //    }
    //    if (type == Byte.class) {
    //      return (T) (value.isMissing() ? null : Byte.valueOf(value.byteValue()));
    //    }
    //
    //    var converter = router.get(type);
    //    if (converter != null) {
    //      return (T) converter.convert(type, value);
    //    }
    //    //    if (value.isSingle()) {
    //    //      for (ValueConverter converter : router.getConverters()) {
    //    //        if (converter.supports(type)) {
    //    //          return (T) converter.convert(value, type);
    //    //        }
    //    //      }
    //    //    } else if (value.isObject()) {
    //    //      for (BeanConverter converter : router.getBeanConverters()) {
    //    //        if (converter.supports(type)) {
    //    //          return (T) converter.convert(value, type);
    //    //        }
    //    //      }
    //    //    }
    //    // Fallback:
    //    ReflectiveBeanConverter reflective = new ReflectiveBeanConverter();
    //    return (T) reflective.convert(value, type, allowEmptyBean);
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
