/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.ArrayValue;
import io.jooby.internal.HashValue;
import io.jooby.internal.MissingValue;
import io.jooby.internal.SingleValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Unified API for HTTP value. This API plays two role:
 *
 * - unify access to HTTP values, like query, path, form and header parameter
 * - works as validation API, because it is able to check for required and type-safe values
 *
 * The value API is composed by three types:
 *
 * - Single value
 * - Object value
 * - Sequence of values (array)
 *
 * Single value are can be converted to string, int, boolean, enum like values.
 * Object value is a key:value structure (like a hash).
 * Sequence of values are index based structure.
 *
 * All these 3 types are modeled into a single Value class. At any time you can treat a value as
 * 1) single, 2) hash or 3) array of them.
 *
 * @since 2.0.0
 * @author edgar
 */
public interface Value extends Iterable<Value> {

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
      } catch (DateTimeParseException expected) {
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
  @Nonnull default String value(@Nonnull String defaultValue) {
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
   * True if this value is an array/sequence (not single or hash).
   *
   * @return True if this value is an array/sequence.
   */
  default boolean isArray() {
    return this instanceof ArrayValue;
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
   * True if this is a hash/object value (not single or array).
   *
   * @return True if this is a hash/object value (not single or array).
   */
  default boolean isObject() {
    return this instanceof HashValue;
  }

  /**
   * True if this is a file upload (not single, not array, not hash).
   *
   * @return True for file upload.
   */
  default boolean isUpload() {
    return this instanceof FileUpload;
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
   * Convert value using the given function.
   *
   * @param fn Function.
   * @param <T> Target type.
   * @return Converted value.
   */
  @Nonnull default <T> T value(@Nonnull SneakyThrows.Function<String, T> fn) {
    return fn.apply(value());
  }

  /**
   * Get string value.
   *
   * @return String value.
   */
  @Nonnull String value();

  /**
   * Get list of values.
   *
   * @return List of values.
   */
  @Nonnull default List<String> toList() {
    return Collections.emptyList();
  }

  /**
   * Get set of values.
   *
   * @return set of values.
   */
  @Nonnull default Set<String> toSet() {
    return Collections.emptySet();
  }

  /**
   * Get list of the given type.
   *
   * @param type Type to convert.
   * @param <T> Item type.
   * @return List of items.
   */
  @Nonnull default <T> List<T> toList(@Nonnull Class<T> type) {
    return Collections.singletonList(to(type));
  }

  /**
   * Get set of the given type.
   *
   * @param type Type to convert.
   * @param <T> Item type.
   * @return Set of items.
   */
  @Nonnull default <T> Set<T> toSet(@Nonnull Class<T> type) {
    return Collections.singleton(to(type));
  }

  /**
   * Convert this value to an Enum.
   *
   * @param fn Mapping function.
   * @param <T> Enum type.
   * @return Enum.
   */
  @Nonnull default <T extends Enum<T>> T toEnum(@Nonnull SneakyThrows.Function<String, T> fn) {
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
  @Nonnull default <T extends Enum<T>> T toEnum(@Nonnull SneakyThrows.Function<String, T> fn,
      @Nonnull Function<String, String> nameProvider) {
    return fn.apply(nameProvider.apply(value()));
  }

  /**
   * Get a value or empty optional.
   *
   * @return Value or empty optional.
   */
  @Nonnull default Optional<String> toOptional() {
    try {
      return Optional.of(value());
    } catch (MissingValueException x) {
      return Optional.empty();
    }
  }

  /**
   * Get a value or empty optional.
   *
   * @param type Item type.
   * @param <T> Item type.
   * @return Value or empty optional.
   */
  @Nonnull default <T> Optional<T> toOptional(@Nonnull Class<T> type) {
    try {
      return Optional.ofNullable(to(type));
    } catch (MissingValueException x) {
      return Optional.empty();
    }
  }

  /* ***********************************************************************************************
   * Node methods
   * ***********************************************************************************************
   */

  /**
   * Get a value at the given position.
   *
   * @param index Position.
   * @return A value at the given position.
   */
  @Nonnull Value get(@Nonnull int index);

  /**
   * Get a value that matches the given name.
   *
   * @param name Field name.
   * @return Field value.
   */
  @Nonnull Value get(@Nonnull String name);

  /**
   * The number of values this one has. For single values size is <code>0</code>.
   *
   * @return Number of values. Mainly for array and hash values.
   */
  default int size() {
    return 0;
  }

  /**
   * Convert this value to the given type. Support values are single-value, array-value and
   * object-value. Object-value can be converted to a JavaBean type.
   *
   * @param type Type to convert.
   * @param <T> Element type.
   * @return Instance of the type.
   */
  @Nonnull <T> T to(@Nonnull Class<T> type);

  /**
   * Value iterator.
   *
   * @return Value iterator.
   */
  @Nonnull default @Override Iterator<Value> iterator() {
    return Collections.emptyIterator();
  }

  /**
   * Name of this value or <code>null</code>.
   *
   * @return Name of this value or <code>null</code>.
   */
  @Nullable String name();

  /**
   * Value as multi-value map.
   *
   * @return Value as multi-value map.
   */
  @Nullable Map<String, List<String>> toMultimap();

  /**
   * Value as single-value map.
   *
   * @return Value as single-value map.
   */
  default @Nonnull Map<String, String> toMap() {
    Map<String, String> map = new LinkedHashMap<>();
    toMultimap().forEach((k, v) -> map.put(k, v.get(0)));
    return map;
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   *   Value value = Value.single("foo", "bar");
   *
   *   String output = value.resolve("${foo}");
   *   System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @return Resolved text.
   */
  @Nonnull default String resolve(@Nonnull String expression) {
    return resolve(expression, "${", "}");
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   *   Value value = Value.single("foo", "bar");
   *
   *   String output = value.resolve("${missing}", true);
   *   System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @param ignoreMissing On missing values, keep the expression as it is.
   * @return Resolved text.
   */
  @Nonnull default String resolve(@Nonnull String expression, boolean ignoreMissing) {
    return resolve(expression, ignoreMissing, "${", "}");
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   *   Value value = Value.single("foo", "bar");
   *
   *   String output = value.resolve("<%missing%>", "<%", "%>");
   *   System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @param startDelim Start delimiter.
   * @param endDelim End delimiter.
   * @return Resolved text.
   */
  @Nonnull default String resolve(@Nonnull String expression, @Nonnull String startDelim,
      @Nonnull String endDelim) {
    return resolve(expression, false, startDelim, endDelim);
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   *   Value value = Value.single("foo", "bar");
   *
   *   String output = value.resolve("<%missing%>", "<%", "%>");
   *   System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @param ignoreMissing On missing values, keep the expression as it is.
   * @param startDelim Start delimiter.
   * @param endDelim End delimiter.
   * @return Resolved text.
   */
  @Nonnull default String resolve(@Nonnull String expression, boolean ignoreMissing,
      @Nonnull String startDelim, @Nonnull String endDelim) {
    if (expression.length() == 0) {
      return "";
    }

    BiFunction<Integer, BiFunction<Integer, Integer, RuntimeException>, RuntimeException> err = (
        start, ex) -> {
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
        throw err.apply(start, (line, column) -> new IllegalArgumentException(
            "found '" + startDelim + "' expecting '" + endDelim + "' at " + line + ":"
                + column));
      }
      buffer.append(expression.substring(offset, start));
      String key = expression.substring(start + startDelim.length(), end);
      String value;
      try {
        // seek
        String[] path = key.split("\\.");
        Value src = path[0].equals(name()) ? this : get(path[0]);
        for (int i = 1; i < path.length; i++) {
          src = src.get(path[i]);
        }
        value = src.value();
      } catch (MissingValueException x) {
        if (ignoreMissing) {
          value = expression.substring(start, end + endDelim.length());
        } else {
          throw err.apply(start, (line, column) -> new NoSuchElementException(
              "Missing " + startDelim + key + endDelim + " at " + line + ":" + column));
        }
      }
      buffer.append(value);
      offset = end + endDelim.length();
      start = expression.indexOf(startDelim, offset);
    }
    if (buffer.length() == 0) {
      return expression;
    }
    if (offset < expression.length()) {
      buffer.append(expression.substring(offset));
    }
    return buffer.toString();
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
  static @Nonnull Value missing(@Nonnull String name) {
    return new MissingValue(name);
  }

  /**
   * Creates a single value.
   *
   * @param name Name of value.
   * @param value Value.
   * @return Single value.
   */
  static @Nonnull Value value(@Nonnull Context ctx, @Nonnull String name, @Nonnull String value) {
    return new SingleValue(ctx, name, value);
  }

  /**
   * Creates a sequence/array of values.
   *
   * @param name Name of array.
   * @param values Field values.
   * @return Array value.
   */
  static @Nonnull Value array(@Nonnull Context ctx, @Nonnull String name,
      @Nonnull List<String> values) {
    return new ArrayValue(ctx, name)
        .add(values);
  }

  /**
   * Creates a value that fits better with the given values.
   *
   * - For null/empty values. It produces a missing value.
   * - For single element (size==1). It produces a single value
   * - For multi-value elements (size&gt;1). It produces an array value.
   *
   * @param name Field name.
   * @param values Field values.
   * @return A value.
   */
  static @Nonnull Value create(Context ctx, @Nonnull String name, @Nullable List<String> values) {
    if (values == null || values.size() == 0) {
      return missing(name);
    }
    if (values.size() == 1) {
      return value(ctx, name, values.get(0));
    }
    return new ArrayValue(ctx, name)
        .add(values);
  }

  /**
   * Creates a value that fits better with the given values.
   *
   * - For null/empty values. It produces a missing value.
   * - For single element (size==1). It produces a single value
   *
   * @param name Field name.
   * @param value Field values.
   * @return A value.
   */
  static @Nonnull Value create(Context ctx, @Nonnull String name, @Nullable String value) {
    if (value == null) {
      return missing(name);
    }
    return value(ctx, name, value);
  }

  /**
   * Create a hash/object value using the map values.
   *
   * @param values Map values.
   * @return A hash/object value.
   */
  static @Nonnull Value hash(Context ctx, @Nonnull Map<String, Collection<String>> values) {
    return new HashValue(ctx, null).put(values);
  }
}
