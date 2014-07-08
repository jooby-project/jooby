package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Predicate;

import jooby.HttpException;
import jooby.HttpStatus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

public enum ParamParser {

  STRING {
    @Override
    public boolean apply(final Type type) {
      return type == String.class;
    }

    @Override
    public Object doParse(final Type type, final List<String> values) {
      return values.get(0);
    }
  },

  BOOLEAN {

    @Override
    public boolean apply(final Type type) {
      return type == boolean.class || type == Boolean.class;
    }

    @Override
    public Object doParse(final Type type, final List<String> values) {
      String value = values.get(0);
      if ("true".equals(value)) {
        return Boolean.TRUE;
      } else if ("false".equals(value)) {
        return Boolean.FALSE;
      }
      throw new IllegalArgumentException("Not a boolean: " + value);
    }

  },

  INT {

    @Override
    public boolean apply(final Type type) {
      return type == int.class || type == Integer.class;
    }

    @Override
    public Object doParse(final Type type, final List<String> values) {
      return Integer.valueOf(values.get(0));
    }
  },

  DOUBLE {

    @Override
    public boolean apply(final Type type) {
      return type == double.class || type == Double.class;
    }

    @Override
    public Object doParse(final Type type, final List<String> values) {
      return Double.valueOf(values.get(0));
    }
  },

  ENUM {
    @Override
    public boolean apply(final Type type) {
      return ifClass(type, Enum.class::isAssignableFrom);
    }

    @SuppressWarnings({"rawtypes", "unchecked" })
    @Override
    public Object doParse(final Type type, final List<String> values) {
      return Enum.valueOf((Class<Enum>) type, values.get(0).toUpperCase());
    }
  },

  FLOAT {
    @Override
    public boolean apply(final Type type) {
      return type == float.class || type == Float.class;
    }

    @Override
    public Object doParse(final Type type, final List<String> values) {
      return Float.valueOf(values.get(0));
    }

  },

  OPTIONAL {
    @Override
    public boolean apply(final Type type) {
      return ifParameterizedType(type, (c) -> c == Optional.class);
    }

    @Override
    public Object parse(final Type type, final List<String> values) throws Exception {
      if (values == null) {
        return Optional.empty();
      }
      return doParse(type, values);
    }

    @Override
    public Object doParse(final Type type, final List<String> values) throws Exception {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type actualType = parameterizedType.getActualTypeArguments()[0];
      return Optional.of(parser(actualType).get().parse(actualType, values));
    }

  },

  COLLECTION {

    private Function<Object, Object> LIST = (arg) -> ImmutableList.copyOf((Object[]) arg);

    private Function<Object, Object> SET = (arg) -> ImmutableSet.copyOf((Object[]) arg);

    private Function<Object, Object> SORTED_SET = (arg) -> ImmutableSortedSet
        .copyOf(Arrays.asList((Object[]) arg));

    private Map<Class<?>, Function<Object, Object>> suppliers = ImmutableMap
        .<Class<?>, Function<Object, Object>> builder()
        .put(List.class, LIST)
        .put(Set.class, SET)
        .put(SortedSet.class, SORTED_SET)
        .build();

    @Override
    public boolean apply(final Type type) {
      return ifParameterizedType(type, suppliers::containsKey);
    }

    @Override
    public Object doParse(final Type type, final List<String> values) throws Exception {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type componentType = parameterizedType.getActualTypeArguments()[0];
      ParamParser parser = parser(componentType).get();
      Object[] args = new Object[values.size()];
      for (int i = 0; i < values.size(); i++) {
        Object value = parser.parse(componentType, ImmutableList.of(values.get(0)));
        args[i] = value;
      }
      return suppliers.get(parameterizedType.getRawType()).apply(args);
    }
  },

  ARRAY {
    @Override
    public boolean apply(final Type type) {
      return ifClass(type, Class::isArray);
    }

    @Override
    public Object doParse(final Type type, final List<String> values) throws Exception {
      Class<?> clazz = (Class<?>) type;
      Class<?> componentType = clazz.getComponentType();
      Object array = Array.newInstance(componentType, values.size());
      ParamParser parser = parser(componentType).get();
      for (int i = 0; i < values.size(); i++) {
        Object value = parser.parse(componentType, ImmutableList.of(values.get(i)));
        Array.set(array, i, value);
      }
      return array;
    }
  },

  FROM_STRING {
    @Override
    public boolean apply(final Type type) {
      return ifClass(type, c -> fromString(c) != null);
    }

    private Method fromString(final Class<?> type) {
      try {
        Method method = type.getDeclaredMethod("fromString", String.class);
        return Modifier.isStatic(method.getModifiers()) ? method : null;
      } catch (NoSuchMethodException | SecurityException ex) {
        return null;
      }
    }

    @Override
    public Object doParse(final Type type, final List<String> values) throws Exception {
      Class<?> clazz = (Class<?>) type;
      return fromString(clazz).invoke(null, values.get(0));
    }
  },

  VALUE_OF {
    @Override
    public boolean apply(final Type type) {
      return ifClass(type, c -> valueOf(c) != null);
    }

    private Method valueOf(final Class<?> type) {
      try {
        Method method = type.getDeclaredMethod("valueOf", String.class);
        return Modifier.isStatic(method.getModifiers()) ? method : null;
      } catch (NoSuchMethodException | SecurityException ex) {
        return null;
      }
    }

    @Override
    public Object doParse(final Type type, final List<String> values) throws Exception {
      Class<?> clazz = (Class<?>) type;
      return valueOf(clazz).invoke(null, values.get(0));
    }
  },

  CHAR {
    @Override
    public boolean apply(final Type type) {
      return type == char.class || type == Character.class;
    }

    @Override
    public Object doParse(final Type type, final List<String> values) {
      return values.get(0).charAt(0);
    }
  },

  BYTE {
    @Override
    public boolean apply(final Type type) {
      return type == byte.class || type == Byte.class;
    }

    @Override
    public Object doParse(final Type type, final List<String> values) {
      return Byte.valueOf(values.get(0));
    }
  },

  SHORT {
    @Override
    public boolean apply(final Type type) {
      return type == byte.class || type == Byte.class;
    }

    @Override
    public Object doParse(final Type type, final List<String> values) {
      return Short.valueOf(values.get(0));
    }
  };

  protected boolean ifClass(final Type type, final Predicate<Class<?>> predicate) {
    if (type instanceof Class) {
      return predicate.test((Class<?>) type);
    }
    return false;
  }

  protected boolean ifParameterizedType(final Type type, final Predicate<Class<?>> predicate) {
    if (type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType) type;
      Type rawType = ptype.getRawType();
      return ifClass(rawType, predicate);
    }
    return false;
  }

  public abstract boolean apply(Type type);

  public Object parse(final Type type, final List<String> values) throws Exception {
    if (values == null) {
      throw new HttpException(HttpStatus.BAD_REQUEST);
    }
    return doParse(type, values);
  }

  public abstract Object doParse(Type type, List<String> values) throws Exception;

  public static Optional<ParamParser> parser(final Type type) {
    requireNonNull(type, "A type is required.");
    for (ParamParser parser : values()) {
      if (parser.apply(type)) {
        return Optional.of(parser);
      }
    }
    return Optional.empty();
  }
}
