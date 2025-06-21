/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Context;
import io.jooby.Formdata;
import io.jooby.SneakyThrows;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.internal.ArrayValue;
import io.jooby.internal.HashValue;
import io.jooby.internal.HeadersValue;
import io.jooby.internal.MissingValue;
import io.jooby.internal.MultipartNode;
import io.jooby.internal.SingleValue;

/**
 * Unified API for HTTP value. This API plays two role:
 *
 * <p>- unify access to HTTP values, like query, path, form and header parameter - works as
 * validation API, because it is able to check for required and type-safe values
 *
 * <p>The value API is composed by three types:
 *
 * <p>- Single value - Object value - Sequence of values (array)
 *
 * <p>Single value are can be converted to string, int, boolean, enum like values. Object value is a
 * key:value structure (like a hash). Sequence of values are index based structure.
 *
 * <p>All these 3 types are modeled into a single Value class. At any time you can treat a value as
 * 1) single, 2) hash or 3) array of them.
 *
 * @since 2.0.0
 * @author edgar
 */
public interface Value extends Iterable<Value> {
  /**
   * Get a value at the given position.
   *
   * @param index Position.
   * @return A value at the given position.
   */
  Value get(int index);

  /**
   * Get a value that matches the given name.
   *
   * @param name Field name.
   * @return Field value.
   */
  Value get(@NonNull String name);

  /**
   * The number of values this one has. For single values size is <code>0</code>.
   *
   * @return Number of values. Mainly for array and hash values.
   */
  int size();

  /**
   * Value iterator.
   *
   * @return Value iterator.
   */
  Iterator<Value> iterator();

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   * Value value = Value.single("foo", "bar");
   *
   * String output = value.resolve("${foo}");
   * System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @return Resolved text.
   */
  default String resolve(@NonNull String expression) {
    return resolve(expression, "${", "}");
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   * Value value = Value.single("foo", "bar");
   *
   * String output = value.resolve("${missing}", true);
   * System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @param ignoreMissing On missing values, keep the expression as it is.
   * @return Resolved text.
   */
  default String resolve(@NonNull String expression, boolean ignoreMissing) {
    return resolve(expression, ignoreMissing, "${", "}");
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   * Value value = Value.single("foo", "bar");
   *
   * String output = value.resolve("<%missing%>", "<%", "%>");
   * System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @param startDelim Start delimiter.
   * @param endDelim End delimiter.
   * @return Resolved text.
   */
  default String resolve(
      @NonNull String expression, @NonNull String startDelim, @NonNull String endDelim) {
    return resolve(expression, false, startDelim, endDelim);
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   * Value value = Value.single("foo", "bar");
   *
   * String output = value.resolve("<%missing%>", "<%", "%>");
   * System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @param ignoreMissing On missing values, keep the expression as it is.
   * @param startDelim Start delimiter.
   * @param endDelim End delimiter.
   * @return Resolved text.
   */
  default String resolve(
      @NonNull String expression,
      boolean ignoreMissing,
      @NonNull String startDelim,
      @NonNull String endDelim) {
    if (expression.isEmpty()) {
      return "";
    }

    BiFunction<Integer, BiFunction<Integer, Integer, RuntimeException>, RuntimeException> err =
        (start, ex) -> {
          String snapshot = expression.substring(0, start);
          int line = Math.max(1, (int) snapshot.chars().filter(ch -> ch == '\n').count());
          int column = start - snapshot.lastIndexOf('\n');
          return ex.apply(line, column);
        };

    StringBuilder buffer = new StringBuilder();
    int offset = 0;
    int start = expression.indexOf(startDelim);
    while (start >= 0) {
      int end = expression.indexOf(endDelim, start + startDelim.length());
      if (end == -1) {
        throw err.apply(
            start,
            (line, column) ->
                new IllegalArgumentException(
                    "found '"
                        + startDelim
                        + "' expecting '"
                        + endDelim
                        + "' at "
                        + line
                        + ":"
                        + column));
      }
      buffer.append(expression.substring(offset, start));
      String key = expression.substring(start + startDelim.length(), end);
      String value;
      try {
        // seek
        String[] path = key.split("\\.");
        var src = path[0].equals(name()) ? this : get(path[0]);
        for (int i = 1; i < path.length; i++) {
          src = src.get(path[i]);
        }
        value = src.value();
      } catch (MissingValueException x) {
        if (ignoreMissing) {
          value = expression.substring(start, end + endDelim.length());
        } else {
          throw err.apply(
              start,
              (line, column) ->
                  new NoSuchElementException(
                      "Missing " + startDelim + key + endDelim + " at " + line + ":" + column));
        }
      }
      buffer.append(value);
      offset = end + endDelim.length();
      start = expression.indexOf(startDelim, offset);
    }
    if (buffer.isEmpty()) {
      return expression;
    }
    if (offset < expression.length()) {
      buffer.append(expression.substring(offset));
    }
    return buffer.toString();
  }

  /**
   * Convert this value to long (if possible).
   *
   * @return Long value.
   */
  default long longValue() {
    try {
      return Long.parseLong(value());
    } catch (NumberFormatException x) {
      try {
        LocalDateTime date = LocalDateTime.parse(value(), Context.RFC1123);
        Instant instant = date.toInstant(ZoneOffset.UTC);
        return instant.toEpochMilli();
      } catch (DateTimeParseException ignored) {
      }
      throw new TypeMismatchException(name(), long.class, x);
    }
  }

  /**
   * Convert this value to long (if possible) or fallback to given value when missing.
   *
   * @param defaultValue Default value.
   * @return Convert this value to long (if possible) or fallback to given value when missing.
   */
  default long longValue(long defaultValue) {
    try {
      return longValue();
    } catch (MissingValueException x) {
      return defaultValue;
    }
  }

  /**
   * Convert this value to int (if possible).
   *
   * @return Int value.
   */
  default int intValue() {
    try {
      return Integer.parseInt(value());
    } catch (NumberFormatException x) {
      throw new TypeMismatchException(name(), int.class, x);
    }
  }

  /**
   * Convert this value to int (if possible) or fallback to given value when missing.
   *
   * @param defaultValue Default value.
   * @return Convert this value to int (if possible) or fallback to given value when missing.
   */
  default int intValue(int defaultValue) {
    try {
      return intValue();
    } catch (MissingValueException x) {
      return defaultValue;
    }
  }

  /**
   * Convert this value to byte (if possible).
   *
   * @return Convert this value to byte (if possible).
   */
  default byte byteValue() {
    try {
      return Byte.parseByte(value());
    } catch (NumberFormatException x) {
      throw new TypeMismatchException(name(), byte.class, x);
    }
  }

  /**
   * Convert this value to byte (if possible) or fallback to given value when missing.
   *
   * @param defaultValue Default value.
   * @return Convert this value to byte (if possible) or fallback to given value when missing.
   */
  default byte byteValue(byte defaultValue) {
    try {
      return byteValue();
    } catch (MissingValueException x) {
      return defaultValue;
    }
  }

  /**
   * Convert this value to float (if possible).
   *
   * @return Convert this value to float (if possible).
   */
  default float floatValue() {
    try {
      return Float.parseFloat(value());
    } catch (NumberFormatException x) {
      throw new TypeMismatchException(name(), float.class, x);
    }
  }

  /**
   * Convert this value to float (if possible) or fallback to given value when missing.
   *
   * @param defaultValue Default value.
   * @return Convert this value to float (if possible) or fallback to given value when missing.
   */
  default float floatValue(float defaultValue) {
    try {
      return floatValue();
    } catch (MissingValueException x) {
      return defaultValue;
    }
  }

  /**
   * Convert this value to double (if possible).
   *
   * @return Convert this value to double (if possible).
   */
  default double doubleValue() {
    try {
      return Double.parseDouble(value());
    } catch (NumberFormatException x) {
      throw new TypeMismatchException(name(), double.class, x);
    }
  }

  /**
   * Convert this value to double (if possible) or fallback to given value when missing.
   *
   * @param defaultValue Default value.
   * @return Convert this value to double (if possible) or fallback to given value when missing.
   */
  default double doubleValue(double defaultValue) {
    try {
      return doubleValue();
    } catch (MissingValueException x) {
      return defaultValue;
    }
  }

  /**
   * Convert this value to boolean (if possible).
   *
   * @return Convert this value to boolean (if possible).
   */
  default boolean booleanValue() {
    return Boolean.parseBoolean(value());
  }

  /**
   * Convert this value to boolean (if possible) or fallback to given value when missing.
   *
   * @param defaultValue Default value.
   * @return Convert this value to boolean (if possible) or fallback to given value when missing.
   */
  default boolean booleanValue(boolean defaultValue) {
    try {
      return booleanValue();
    } catch (MissingValueException x) {
      return defaultValue;
    }
  }

  /**
   * Convert this value to String (if possible) or fallback to given value when missing.
   *
   * @param defaultValue Default value.
   * @return Convert this value to String (if possible) or fallback to given value when missing.
   */
  default String value(@NonNull String defaultValue) {
    try {
      return value();
    } catch (MissingValueException x) {
      return defaultValue;
    }
  }

  /**
   * Convert this value to String (if possible) or <code>null</code> when missing.
   *
   * @return Convert this value to String (if possible) or <code>null</code> when missing.
   */
  @Nullable default String valueOrNull() {
    return value((String) null);
  }

  /**
   * Convert value using the given function.
   *
   * @param fn Function.
   * @param <T> Target type.
   * @return Converted value.
   */
  default <T> T value(@NonNull SneakyThrows.Function<String, T> fn) {
    return fn.apply(value());
  }

  /**
   * Get string value.
   *
   * @return String value.
   */
  String value();

  /**
   * Get list of values.
   *
   * @return List of values.
   */
  List<String> toList();

  /**
   * Get set of values.
   *
   * @return set of values.
   */
  Set<String> toSet();

  /**
   * Convert this value to an Enum.
   *
   * @param fn Mapping function.
   * @param <T> Enum type.
   * @return Enum.
   */
  default <T extends Enum<T>> T toEnum(@NonNull SneakyThrows.Function<String, T> fn) {
    return toEnum(fn, String::toUpperCase);
  }

  /**
   * Convert this value to an Enum.
   *
   * @param fn Mapping function.
   * @param nameProvider Enum name provider.
   * @param <T> Enum type.
   * @return Enum.
   */
  default <T extends Enum<T>> T toEnum(
      @NonNull SneakyThrows.Function<String, T> fn,
      @NonNull Function<String, String> nameProvider) {
    return fn.apply(nameProvider.apply(value()));
  }

  /**
   * Get a value or empty optional.
   *
   * @return Value or empty optional.
   */
  default Optional<String> toOptional() {
    try {
      return Optional.of(value());
    } catch (MissingValueException x) {
      return Optional.empty();
    }
  }

  /**
   * True if this is a single value (not a hash or array).
   *
   * @return True if this is a single value (not a hash or array).
   */
  default boolean isSingle() {
    return this instanceof SingleValue;
  }

  /**
   * True for missing values.
   *
   * @return True for missing values.
   */
  default boolean isMissing() {
    return this instanceof MissingValue;
  }

  /**
   * True for present values.
   *
   * @return True for present values.
   */
  default boolean isPresent() {
    return !isMissing();
  }

  /**
   * True if this value is an array/sequence (not single or hash).
   *
   * @return True if this value is an array/sequence.
   */
  default boolean isArray() {
    return this instanceof ArrayValue;
  }

  /**
   * True if this is a hash/object value (not single or array).
   *
   * @return True if this is a hash/object value (not single or array).
   */
  default boolean isObject() {
    return this instanceof HashValue;
  }

  /**
   * Name of this value or <code>empty string</code> for root hash.
   *
   * @return Name of this value or <code>empty string</code> for root hash.
   */
  @Nullable String name();

  /**
   * Get a value or empty optional.
   *
   * @param type Item type.
   * @param <T> Item type.
   * @return Value or empty optional.
   */
  default <T> Optional<T> toOptional(@NonNull Class<T> type) {
    try {
      return Optional.ofNullable(toNullable(type));
    } catch (MissingValueException x) {
      return Optional.empty();
    }
  }

  /**
   * Get list of the given type.
   *
   * @param type Type to convert.
   * @param <T> Item type.
   * @return List of items.
   */
  default <T> List<T> toList(@NonNull Class<T> type) {
    return List.of(to(type));
  }

  /**
   * Get set of the given type.
   *
   * @param type Type to convert.
   * @param <T> Item type.
   * @return Set of items.
   */
  default <T> Set<T> toSet(@NonNull Class<T> type) {
    return Set.of(to(type));
  }

  /**
   * Convert this value to the given type. Support values are single-value, array-value and
   * object-value. Object-value can be converted to a JavaBean type.
   *
   * <p>At least one of the property of the node must match a target type property.
   *
   * @param type Type to convert.
   * @param <T> Element type.
   * @return Instance of the type.
   */
  <T> T to(@NonNull Class<T> type);

  /**
   * Convert this value to the given type. Support values are single-value, array-value and
   * object-value. Object-value can be converted to a JavaBean type.
   *
   * @param type Type to convert.
   * @param <T> Element type.
   * @return Instance of the type or <code>null</code>.
   */
  @Nullable <T> T toNullable(@NonNull Class<T> type);

  /**
   * Value as multi-value map.
   *
   * @return Value as multi-value map.
   */
  Map<String, List<String>> toMultimap();

  /**
   * Value as single-value map.
   *
   * @return Value as single-value map.
   */
  default Map<String, String> toMap() {
    Map<String, String> map = new LinkedHashMap<>();
    toMultimap().forEach((k, v) -> map.put(k, v.get(0)));
    return map;
  }

  /* ***********************************************************************************************
   * Factory methods
   * ***********************************************************************************************
   */

  /**
   * Creates a missing value.
   *
   * @param name Name of missing value.
   * @return Missing value.
   */
  static Value missing(@NonNull String name) {
    return new MissingValue(name);
  }

  /**
   * Creates a single value.
   *
   * @param valueFactory Current context.
   * @param name Name of value.
   * @param value Value.
   * @return Single value.
   */
  static Value value(
      @NonNull ValueFactory valueFactory, @NonNull String name, @NonNull String value) {
    return new SingleValue(valueFactory, name, value);
  }

  /**
   * Creates a sequence/array of values.
   *
   * @param valueFactory Current context.
   * @param name Name of array.
   * @param values Field values.
   * @return Array value.
   */
  static Value array(
      @NonNull ValueFactory valueFactory, @NonNull String name, @NonNull List<String> values) {
    return new ArrayValue(valueFactory, name).add(values);
  }

  /**
   * Creates a value that fits better with the given values.
   *
   * <p>- For null/empty values. It produces a missing value. - For single element (size==1). It
   * produces a single value - For multi-value elements (size&gt;1). It produces an array value.
   *
   * @param valueFactory Current context.
   * @param name Field name.
   * @param values Field values.
   * @return A value.
   */
  static Value create(
      @NonNull ValueFactory valueFactory, @NonNull String name, @Nullable List<String> values) {
    if (values == null || values.isEmpty()) {
      return missing(name);
    }
    if (values.size() == 1) {
      return value(valueFactory, name, values.get(0));
    }
    return new ArrayValue(valueFactory, name).add(values);
  }

  /**
   * Creates a value that fits better with the given values.
   *
   * <p>- For null/empty values. It produces a missing value. - For single element (size==1). It
   * produces a single value
   *
   * @param valueFactory Current context.
   * @param name Field name.
   * @param value Field values.
   * @return A value.
   */
  static Value create(
      @NonNull ValueFactory valueFactory, @NonNull String name, @Nullable String value) {
    if (value == null) {
      return missing(name);
    }
    return value(valueFactory, name, value);
  }

  /**
   * Create a hash/object value using the map values.
   *
   * @param valueFactory Current context.
   * @param values Map values.
   * @return A hash/object value.
   */
  static Value hash(
      @NonNull ValueFactory valueFactory, @NonNull Map<String, Collection<String>> values) {
    var node = new HashValue(valueFactory, null);
    node.put(values);
    return node;
  }

  /**
   * Creates a formdata.
   *
   * @param valueFactory Current context.
   * @return A hash/object value.
   */
  static Formdata formdata(@NonNull ValueFactory valueFactory) {
    return new MultipartNode(valueFactory);
  }

  /**
   * Create a hash/object value using the map values.
   *
   * @param valueFactory Current context.
   * @param values Map values.
   * @return A hash/object value.
   */
  static Value headers(
      @NonNull ValueFactory valueFactory, @NonNull Map<String, Collection<String>> values) {
    var node = new HeadersValue(valueFactory);
    node.put(values);
    return node;
  }
}
