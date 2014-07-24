package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BiFunction;

import jooby.HttpException;
import jooby.HttpField;
import jooby.HttpStatus;
import jooby.ThrowingSupplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.Invokable;
import com.google.inject.TypeLiteral;

public class GetterImpl implements HttpField {

  private static final Object[] EMPTY_ARRAY = new Object[0];

  private static Map<Object, BiFunction<String, List<String>, Object>> converters = new HashMap<>();

  static {
    converters.put(Boolean.class, GetterImpl::asBoolean);
    converters.put(Byte.class, GetterImpl::asByte);
    converters.put(Short.class, GetterImpl::asShort);
    converters.put(Integer.class, GetterImpl::asInt);
    converters.put(Long.class, GetterImpl::asLong);
    converters.put(Float.class, GetterImpl::asFloat);
    converters.put(Double.class, GetterImpl::asDouble);
    converters.put(String.class, GetterImpl::asString);

  }

  protected final String name;

  protected final List<String> values;

  public GetterImpl(final String name, final List<String> values) {
    this.name = requireNonNull(name, "Parameter's name is missing.");
    this.values = values;
  }

  @Override
  public boolean getBoolean() {
    return asBoolean(name, values);
  }

  private static boolean asBoolean(final String name, final List<String> values) {
    failOnEmpty(name, values);
    String value = values.get(0);
    if ("true".equals(value)) {
      return Boolean.TRUE;
    } else if ("false".equals(value)) {
      return Boolean.FALSE;
    }
    throw new IllegalArgumentException("Not a boolean: " + value);
  }

  @Override
  public byte getByte() {
    return asByte(name, values);
  }

  private static byte asByte(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Byte.valueOf(values.get(0));
  }

  @Override
  public short getShort() {
    return asShort(name, values);
  }

  private static short asShort(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Short.valueOf(values.get(0));
  }

  @Override
  public int getInt() {
    return asInt(name, values);
  }

  private static int asInt(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Integer.valueOf(values.get(0));
  }

  @Override
  public long getLong() {
    return asLong(name, values);
  }

  private static long asLong(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Long.valueOf(values.get(0));
  }

  @Override
  public float getFloat() {
    return asFloat(name, values);
  }

  private static float asFloat(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Float.valueOf(values.get(0));
  }

  @Override
  public double getDouble() {
    return asDouble(name, values);
  }

  private static double asDouble(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Double.valueOf(values.get(0));
  }

  @Override
  public <T extends Enum<T>> T getEnum(final Class<T> type) {
    requireNonNull(type, "An enum type is required.");
    failOnEmpty(name, values);
    return Enum.valueOf(type, values.get(0).toUpperCase());
  }

  @Override
  public <T> List<T> getList(final Class<T> type) {
    return ImmutableList.copyOf(asArray(type));
  }

  @Override
  public <T> Set<T> getSet(final Class<T> type) {
    return ImmutableSet.copyOf(asArray(type));
  }

  @Override
  public <T extends Comparable<T>> SortedSet<T> getSortedSet(final Class<T> type) {
    T[] array = asArray(type);
    return ImmutableSortedSet.copyOf(array);
  }

  private Object[] asArrayOf(final Class<?> type) {
    if (values == null || values.size() == 0) {
      return EMPTY_ARRAY;
    }
    Class<?> componentType = Primitives.wrap(type);
    BiFunction<String, List<String>, Object> converter = converter(type);
    Object array = Array.newInstance(componentType, values.size());
    for (int i = 0; i < values.size(); i++) {
      Object value = converter.apply(name, ImmutableList.of(values.get(i)));
      Array.set(array, i, value);

    }
    return (Object[]) array;
  }

  @SuppressWarnings("unchecked")
  private <T> T[] asArray(final Class<T> type) {
    Object[] array = asArrayOf(type);
    return (T[]) array;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> getOptional(final Class<T> type) {
    if (values == null || values.size() == 0) {
      return Optional.empty();
    }
    BiFunction<String, List<String>, Object> converter = converter(type);
    return (Optional<T>) Optional.of(converter.apply(name, values));
  }

  @Override
  public String getString() {
    return asString(name, values);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T get(final TypeLiteral<T> type) {
    BiFunction<String, List<String>, Object> converter = converter(type);
    return (T) converter.apply(name, values);
  }

  private static String asString(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return values.get(0);
  }

  private static BiFunction<String, List<String>, Object> converter(final Class<?> type) {
    return converter(TypeLiteral.get(type));
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private static BiFunction<String, List<String>, Object> converter(final TypeLiteral<?> literal) {
    Class<?> rawType = literal.getRawType();
    Class<?> wrapType = Primitives.wrap(rawType);
    BiFunction<String, List<String>, Object> converter = converters.get(wrapType);
    if (converter == null) {
      if (Optional.class.isAssignableFrom(rawType)) {
        converter = (name, values) -> new GetterImpl(name, values)
            .getOptional(classFrom(literal));
      } else if (Enum.class.isAssignableFrom(rawType)) {
        converter = (name, values) -> new GetterImpl(name, values)
            .getEnum((Class<Enum>) rawType);
      } else if (List.class.isAssignableFrom(rawType)) {
        converter = (name, values) -> new GetterImpl(name, values)
            .getList(classFrom(literal));
      } else if (Set.class.isAssignableFrom(rawType)) {
        if (SortedSet.class.isAssignableFrom(rawType)) {
          converter = (name, values) -> new GetterImpl(name, values)
              .getSortedSet((Class) classFrom(literal));
        } else {
          converter = (name, values) -> new GetterImpl(name, values)
              .getSet(classFrom(literal));
        }
      } else {
        // String constructor
        Invokable<?, ?> exec = invokable(rawType);
        if (exec != null) {
          converter = (name, values) -> handleThrowable(() -> exec.invoke(null, values.get(0)));
        } else {
          throw new HttpException(HttpStatus.BAD_REQUEST, "Unknown parameter type: " + rawType);
        }
      }
    }
    return converter;
  }

  private static Invokable<?, ?> invokable(final Class<?> type) {
    try {
      return Invokable.from(type.getDeclaredConstructor(String.class));
    } catch (NoSuchMethodException | SecurityException ex) {
      try {
        return Invokable.from(type.getDeclaredMethod("valueOf", String.class));
      } catch (NoSuchMethodException | SecurityException ex1) {
        try {
          return Invokable.from(type.getDeclaredMethod("fromString", String.class));
        } catch (NoSuchMethodException | SecurityException ex2) {
          return null;
        }
      }
    }
  }

  private static Object handleThrowable(final ThrowingSupplier<?> supplier) {
    try {
      return supplier.get();
    } catch (HttpException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new HttpException(HttpStatus.BAD_REQUEST, ex);
    }
  }

  private static Class<?> classFrom(final TypeLiteral<?> type) {
    return classFrom(type.getType());
  }

  private static Class<?> classFrom(final Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type actualType = parameterizedType.getActualTypeArguments()[0];
      return classFrom(actualType);
    }
    throw new HttpException(HttpStatus.BAD_REQUEST, "Unknown type: " + type);
  }

  private static void failOnNull(final String name, final List<?> values) {
    if (values == null) {
      throw new HttpException(HttpStatus.BAD_REQUEST, "Value not found: " + name);
    }
  }

  private static void failOnEmpty(final String name, final List<?> values) {
    failOnNull(name, values);
    if (values.size() == 0) {
      throw new HttpException(HttpStatus.BAD_REQUEST, "Value not found: " + name);
    }
  }
}
