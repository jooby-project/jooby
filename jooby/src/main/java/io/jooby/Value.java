/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.ValueInjector;
import io.jooby.internal.UrlParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

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

  class Sequence implements Value {
    private final String name;

    private final List<Value> value = new ArrayList<>(5);

    public Sequence(String name) {
      this.name = name;
    }

    @Override public String name() {
      return name;
    }

    public Sequence add(Value value) {
      this.value.add(value);
      return this;
    }

    public Sequence add(String value) {
      return this.add(new Single(name, value));
    }

    @Override public Value get(@Nonnull int index) {
      try {
        return value.get(index);
      } catch (IndexOutOfBoundsException x) {
        return new Missing(name + "[" + index + "]");
      }
    }

    @Override public Value get(@Nonnull String name) {
      return new Missing(this.name + "." + name);
    }

    @Override public int size() {
      return value.size();
    }

    @Override public String value() {
      String name = name();
      throw new Err.TypeMismatch(name == null ? getClass().getSimpleName() : name, String.class);
    }

    @Override public String toString() {
      return value.toString();
    }

    @Override public Iterator<Value> iterator() {
      return value.iterator();
    }

    @Override public Map<String, List<String>> toMultimap() {
      List<String> values = new ArrayList<>();
      value.stream().forEach(it -> it.toMultimap().values().forEach(values::addAll));
      return Collections.singletonMap(name, values);
    }

    @Override public List<String> toList() {
      return fill(new ArrayList<>());
    }

    @Override public Set<String> toSet() {
      return fill(new LinkedHashSet<>());
    }

    private <C extends Collection<String>> C fill(C values) {
      value.forEach(v -> values.addAll(v.toList()));
      return values;
    }
  }

  class Hash implements Value {
    private static final Map<String, Value> EMPTY = Collections.emptyMap();

    private Map<String, Value> hash = EMPTY;

    private final String name;

    protected Hash(String name) {
      this.name = name;
    }

    protected Hash() {
      this.name = null;
    }

    @Override public String name() {
      return name;
    }

    public Hash put(String path, String value) {
      return put(path, Collections.singletonList(value));
    }

    public Hash put(String path, FileUpload upload) {
      put(path, (name, scope) -> {
        Value existing = scope.get(name);
        if (existing == null) {
          scope.put(name, upload);
        } else {
          Sequence list;
          if (existing instanceof Sequence) {
            list = (Sequence) existing;
          } else {
            list = new Sequence(name).add(existing);
            scope.put(name, list);
          }
          list.add(upload);
        }
      });
      return this;
    }

    public Hash put(String path, Collection<String> values) {
      put(path, (name, scope) -> {
        for (String value : values) {
          Value existing = scope.get(name);
          if (existing == null) {
            scope.put(name, new Single(name, value));
          } else {
            Sequence list;
            if (existing instanceof Sequence) {
              list = (Sequence) existing;
            } else {
              list = new Sequence(name).add(existing);
              scope.put(name, list);
            }
            list.add(value);
          }
        }
      });
      return this;
    }

    private void put(String path, BiConsumer<String, Map<String, Value>> consumer) {
      // Locate node:
      int nameStart = 0;
      int nameEnd = path.length();
      Hash target = this;
      for (int i = nameStart; i < nameEnd; i++) {
        char ch = path.charAt(i);
        if (ch == '.') {
          String name = path.substring(nameStart, i);
          nameStart = i + 1;
          target = target.getOrCreateScope(name);
        } else if (ch == '[') {
          if (nameStart < i) {
            String name = path.substring(nameStart, i);
            target = target.getOrCreateScope(name);
          }
          nameStart = i + 1;
        } else if (ch == ']') {
          if (i + 1 < nameEnd) {
            String name = path.substring(nameStart, i);
            if (isNumber(name)) {
              target.useIndexes();
            }
            nameStart = i + 1;
            target = target.getOrCreateScope(name);
          } else {
            nameEnd = i;
          }
        }
      }
      String key = path.substring(nameStart, nameEnd);
      if (isNumber(key)) {
        target.useIndexes();
      }
      // Final node
      consumer.accept(key, target.hash());
    }

    private void useIndexes() {
      TreeMap<String, Value> ordered = new TreeMap<>();
      ordered.putAll(hash);
      hash.clear();
      this.hash = ordered;
    }

    private boolean isNumber(String value) {
      for (int i = 0; i < value.length(); i++) {
        if (!Character.isDigit(value.charAt(i))) {
          return false;
        }
      }
      return true;
    }

    private Map<String, Value> hash() {
      if (hash == EMPTY) {
        hash = new LinkedHashMap<>();
      }
      return hash;
    }

    /*package*/ Hash getOrCreateScope(String name) {
      return (Hash) hash().computeIfAbsent(name, Hash::new);
    }

    public Value get(@Nonnull String name) {
      Value value = hash.get(name);
      if (value == null) {
        return new Missing(scope(name));
      }
      return value;
    }

    private String scope(String name) {
      return this.name == null ? name : this.name + "." + name;
    }

    @Override public Value get(@Nonnull int index) {
      return get(Integer.toString(index));
    }

    public int size() {
      return hash.size();
    }

    @Override public String value() {
      String name = name();
      throw new Err.TypeMismatch(name == null ? getClass().getSimpleName() : name, String.class);
    }

    @Override public Iterator<Value> iterator() {
      return hash.values().iterator();
    }

    @Override public Map<String, List<String>> toMultimap() {
      Map<String, List<String>> result = new LinkedHashMap<>(hash.size());
      Set<Map.Entry<String, Value>> entries = hash.entrySet();
      String scope = name == null ? "" : name + ".";
      for (Map.Entry<String, Value> entry : entries) {
        Value value = entry.getValue();
        if (!value.isUpload()) {
          value.toMultimap().forEach((k, v) -> {
            result.put(scope + k, v);
          });
        }
      }
      return result;
    }

    @Override public String toString() {
      return hash.toString();
    }
  }

  class Missing implements Value {
    private String name;

    public Missing(String name) {
      this.name = name;
    }

    @Override public String name() {
      return name;
    }

    @Override public Value get(@Nonnull String name) {
      return this.name.equals(name) ? this : new Missing(this.name + "." + name);
    }

    @Override public Value get(@Nonnull int index) {
      return new Missing(this.name + "[" + index + "]");
    }

    @Override public String value() {
      throw new Err.Missing(name);
    }

    @Override public Map<String, List<String>> toMultimap() {
      return Collections.emptyMap();
    }
  }

  class Single implements Value {

    private final String name;

    private final String value;

    public Single(String name, String value) {
      this.name = name;
      this.value = value;
    }

    @Override public String name() {
      return name;
    }

    @Override public Value get(@Nonnull int index) {
      return index == 0 ? this : get(Integer.toString(index));
    }

    @Override public Value get(@Nonnull String name) {
      return new Missing(this.name + "." + name);
    }

    @Override public int size() {
      return 1;
    }

    @Override public String value() {
      return value;
    }

    @Override public String toString() {
      return value;
    }

    @Override public Iterator<Value> iterator() {
      return Collections.<Value>singletonList(this).iterator();
    }

    @Override public Map<String, List<String>> toMultimap() {
      return singletonMap(name, singletonList(value));
    }

    @Override public List<String> toList() {
      return singletonList(value);
    }

    @Override public Set<String> toSet() {
      return singleton(value);
    }
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
      } catch (DateTimeParseException expected) {
      }
      throw new Err.TypeMismatch(name(), long.class, x);
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
    } catch (Err.Missing x) {
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
      throw new Err.TypeMismatch(name(), int.class, x);
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
    } catch (Err.Missing x) {
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
      throw new Err.TypeMismatch(name(), byte.class, x);
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
    } catch (Err.Missing x) {
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
      throw new Err.TypeMismatch(name(), float.class, x);
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
    } catch (Err.Missing x) {
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
      throw new Err.TypeMismatch(name(), double.class, x);
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
    } catch (Err.Missing x) {
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
    } catch (Err.Missing x) {
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
    } catch (Err.Missing x) {
      return defaultValue;
    }
  }

  /**
   * Convert this value to String (if possible) or <code>null</code> when missing.
   *
   * @return Convert this value to String (if possible) or <code>null</code> when missing.
   */
  @Nonnull default String valueOrNull() {
    return value((String) null);
  }

  /**
   * True if this value is an array/sequence (not single or hash).
   *
   * @return True if this value is an array/sequence.
   */
  default boolean isArray() {
    return this instanceof Sequence;
  }

  /**
   * True if this is a single value (not a hash or array).
   *
   * @return True if this is a single value (not a hash or array).
   */
  default boolean isSingle() {
    return this instanceof Single;
  }

  /**
   * True if this is a hash/object value (not single or array).
   *
   * @return True if this is a hash/object value (not single or array).
   */
  default boolean isObject() {
    return this instanceof Hash;
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
    return this instanceof Missing;
  }

  /**
   * Convert value using the given function.
   *
   * @param fn Function.
   * @param <T> Target type.
   * @return Converted value.
   */
  @Nonnull default <T> T value(@Nonnull Throwing.Function<String, T> fn) {
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
    return to(Reified.list(type));
  }

  /**
   * Get set of the given type.
   *
   * @param type Type to convert.
   * @param <T> Item type.
   * @return Set of items.
   */
  @Nonnull default <T> Set<T> toSet(@Nonnull Class<T> type) {
    return to(Reified.set(type));
  }

  /**
   * Convert this value to an Enum.
   *
   * @param fn Mapping function.
   * @param <T> Enum type.
   * @return Enum.
   */
  @Nonnull default <T extends Enum<T>> T toEnum(@Nonnull Throwing.Function<String, T> fn) {
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
  @Nonnull default <T extends Enum<T>> T toEnum(@Nonnull Throwing.Function<String, T> fn,
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
    } catch (Err.Missing x) {
      return Optional.empty();
    }
  }

  /**
   * Get a value or empty optional.
   *
   * @return Value or empty optional.
   */
  @Nonnull default <T> Optional<T> toOptional(@Nonnull Class<T> type) {
    return to(Reified.optional(type));
  }

  /**
   * Get a file upload from this value.
   *
   * @return A file upload.
   */
  default FileUpload fileUpload() {
    throw new Err.TypeMismatch(name(), FileUpload.class);
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
  @Nonnull default <T> T to(@Nonnull Class<T> type) {
    return new ValueInjector().inject(this, type, type);
  }

  /**
   * Convert this value to the given type. Support values are single-value, array-value and
   * object-value. Object-value can be converted to a JavaBean type.
   *
   * @param type Type to convert.
   * @param <T> Element type.
   * @return Instance of the type.
   */
  @Nonnull default <T> T to(@Nonnull Type type) {
    return new ValueInjector().inject(this, type, Reified.rawType(type));
  }

  /**
   * Convert this value to the given type. Support values are single-value, array-value and
   * object-value. Object-value can be converted to a JavaBean type.
   *
   * @param type Type to convert.
   * @param <T> Element type.
   * @return Instance of the type.
   */
  @Nonnull default <T> T to(@Nonnull Reified<T> type) {
    return new ValueInjector().inject(this, type.getType(), type.getRawType());
  }

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
      } catch (Err.Missing x) {
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
    return new Missing(name);
  }

  /**
   * Creates a single value.
   *
   * @param name Name of value.
   * @param value Value.
   * @return Single value.
   */
  static @Nonnull Value value(@Nonnull String name, @Nonnull String value) {
    return new Single(name, value);
  }

  /**
   * Creates a sequence/array of values.
   *
   * @param name Name of array.
   * @return Array value.
   */
  static @Nonnull Value array(@Nonnull String name) {
    return new Sequence(name);
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
  static @Nonnull Value create(@Nonnull String name, @Nullable List<String> values) {
    if (values == null || values.size() == 0) {
      return missing(name);
    }
    if (values.size() == 1) {
      return value(name, values.get(0));
    }
    Sequence array = new Sequence(name);
    for (String value : values) {
      array.add(value);
    }
    return array;
  }

  static Hash headers() {
    return new Hash(null);
  }

  static Hash path(Map<String, String> params) {
    Hash path = new Hash(null);
    params.forEach(path::put);
    return path;
  }

  static QueryString queryString(@Nonnull String queryString) {
    return UrlParser.queryString(queryString);
  }
}
