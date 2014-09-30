package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BiFunction;

import jooby.HttpException;
import jooby.HttpStatus;
import jooby.Variant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.primitives.Primitives;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeConverterBinding;

public class VariantImpl implements Variant {

  private static final Object[] EMPTY_ARRAY = new Object[0];

  private static Map<Object, BiFunction<String, List<String>, Object>> primiteConverters = new HashMap<>();

  static {
    primiteConverters.put(Boolean.class, VariantImpl::toBoolean);
    primiteConverters.put(Byte.class, VariantImpl::toByte);
    primiteConverters.put(Short.class, VariantImpl::toShort);
    primiteConverters.put(Integer.class, VariantImpl::toInt);
    primiteConverters.put(Long.class, VariantImpl::toLong);
    primiteConverters.put(Float.class, VariantImpl::toFloat);
    primiteConverters.put(Double.class, VariantImpl::toDouble);
    primiteConverters.put(String.class, VariantImpl::stringValue);
  }

  protected final String name;

  protected final List<String> values;

  private Set<TypeConverterBinding> typeConverters;

  VariantImpl(final String name, final List<String> values,
      final Set<TypeConverterBinding> typeConverters) {
    this.name = requireNonNull(name, "Parameter's name is missing.");
    this.values = values;
    this.typeConverters = requireNonNull(typeConverters, "Type converters are required.");
  }

  @Override
  public boolean booleanValue() {
    return toBoolean(name, values);
  }

  private static boolean toBoolean(final String name, final List<String> values) {
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
  public byte byteValue() {
    return toByte(name, values);
  }

  private static byte toByte(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Byte.valueOf(values.get(0));
  }

  @Override
  public short shortValue() {
    return toShort(name, values);
  }

  private static short toShort(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Short.valueOf(values.get(0));
  }

  @Override
  public int intValue() {
    return toInt(name, values);
  }

  private static int toInt(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Integer.valueOf(values.get(0));
  }

  @Override
  public long longValue() {
    return toLong(name, values);
  }

  private static long toLong(final String name, final List<String> values) {
    failOnEmpty(name, values);
    try {
      return Long.valueOf(values.get(0));
    } catch (NumberFormatException ex) {
      // Is a date?
      try {
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        LocalDateTime date = LocalDateTime.parse(values.get(0), formatter);
        Instant instant = date.toInstant(ZoneOffset.UTC);
        return instant.toEpochMilli();
      } catch (DateTimeParseException ignored) {
        throw ex;
      }

    }
  }

  @Override
  public float floatValue() {
    return toFloat(name, values);
  }

  private static float toFloat(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Float.valueOf(values.get(0));
  }

  @Override
  public double doubleValue() {
    return toDouble(name, values);
  }

  private static double toDouble(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return Double.valueOf(values.get(0));
  }

  @Override
  public <T extends Enum<T>> T enumValue(final Class<T> type) {
    requireNonNull(type, "An enum type is required.");
    failOnEmpty(name, values);
    return Enum.valueOf(type, values.get(0));
  }

  @Override
  public <T> List<T> toList(final Class<T> type) {
    return ImmutableList.copyOf(asArray(type));
  }

  @Override
  public <T> Set<T> toSet(final Class<T> type) {
    return ImmutableSet.copyOf(asArray(type));
  }

  @Override
  public <T extends Comparable<T>> SortedSet<T> toSortedSet(final Class<T> type) {
    T[] array = asArray(type);
    return ImmutableSortedSet.copyOf(array);
  }

  private Object[] asArrayOf(final Class<?> type, final Set<TypeConverterBinding> typeConverters) {
    if (values == null || values.size() == 0) {
      return EMPTY_ARRAY;
    }
    Class<?> componentType = Primitives.wrap(type);
    BiFunction<String, List<String>, Object> converter = converter(type, typeConverters);
    Object array = Array.newInstance(componentType, values.size());
    for (int i = 0; i < values.size(); i++) {
      Object value = converter.apply(name, ImmutableList.of(values.get(i)));
      Array.set(array, i, value);

    }
    return (Object[]) array;
  }

  @SuppressWarnings("unchecked")
  private <T> T[] asArray(final Class<T> type) {
    Object[] array = asArrayOf(type, typeConverters);
    return (T[]) array;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> toOptional(final Class<T> type) {
    if (values == null || values.size() == 0) {
      return Optional.empty();
    }
    BiFunction<String, List<String>, Object> converter = converter(type, typeConverters);
    return (Optional<T>) Optional.of(converter.apply(name, values));
  }

  @Override
  public String stringValue() {
    return stringValue(name, values);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T to(final TypeLiteral<T> type) {
    BiFunction<String, List<String>, Object> converter = converter(type, typeConverters);
    return (T) converter.apply(name, values);
  }

  // @Override
  // public String toString() {
  // if (values == null || values.size() == 0) {
  // return "MISSING";
  // }
  // if (values.size() == 1) {
  // return values.get(0);
  // }
  // return values.toString();
  // }

  private static String stringValue(final String name, final List<String> values) {
    failOnEmpty(name, values);
    return values.get(0);
  }

  private static BiFunction<String, List<String>, Object> converter(final Class<?> type,
      final Set<TypeConverterBinding> typeConverters) {
    return converter(TypeLiteral.get(type), typeConverters);
  }

  private static BiFunction<String, List<String>, Object> converter(final TypeLiteral<?> literal,
      final Set<TypeConverterBinding> typeConverters) {
    Class<?> rawType = literal.getRawType();
    Class<?> wrapType = Primitives.wrap(rawType);
    BiFunction<String, List<String>, Object> converter = primiteConverters.get(wrapType);
    if (converter == null) {
      return complexConverters(literal, typeConverters);
    }
    return converter;
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private static BiFunction<String, List<String>, Object> complexConverters(
      final TypeLiteral<?> literal, final Set<TypeConverterBinding> typeConverters) {
    Class<?> rawType = literal.getRawType();
    if (Optional.class.isAssignableFrom(rawType)) {
      return (name, values) -> new VariantImpl(name, values, typeConverters)
          .toOptional(classFrom(literal));
    } else if (Enum.class.isAssignableFrom(rawType)) {
      return (name, values) -> new VariantImpl(name, values, typeConverters)
          .enumValue((Class<Enum>) rawType);
    } else if (List.class.isAssignableFrom(rawType)) {
      return (name, values) -> new VariantImpl(name, values, typeConverters)
          .toList(classFrom(literal));
    } else if (Set.class.isAssignableFrom(rawType)) {
      if (SortedSet.class.isAssignableFrom(rawType)) {
        return (name, values) -> new VariantImpl(name, values, typeConverters)
            .toSortedSet((Class) classFrom(literal));
      } else {
        return (name, values) -> new VariantImpl(name, values, typeConverters)
            .toSet(classFrom(literal));
      }
    } else {
      // Guice type converter
      return typeConverters
          .stream()
          .filter(c -> c.getTypeMatcher().matches(literal))
          .findFirst()
          .map(c -> {
            BiFunction<String, List<String>, Object> fn = (name, values) -> c.getTypeConverter()
                .convert(values.get(0), literal);
            return fn;
          })
          .orElseThrow(
              () -> new HttpException(HttpStatus.BAD_REQUEST, "Unknown parameter type: " + rawType)
          );
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

  private static void failOnEmpty(final String name, final List<?> values) {
    if (values == null || values.size() == 0) {
      throw new HttpException(HttpStatus.BAD_REQUEST, "Missing value: " + name);
    }
  }
}
