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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public interface Value extends Iterable<Value> {

  class Array implements Value {
    private final String name;

    private final List<Value> value = new ArrayList<>(5);

    public Array(String name) {
      this.name = name;
    }

    @Override public String name() {
      return name;
    }

    public Array add(Value value) {
      this.value.add(value);
      return this;
    }

    public Array add(String value) {
      return this.add(new Simple(name, value));
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
      throw new Err.BadRequest("Type mismatch: cannot convert array to string");
    }

    @Override public String toString() {
      return value.toString();
    }

    @Override public Iterator<Value> iterator() {
      return value.iterator();
    }

    @Override public Map<String, List<String>> toMap() {
      List<String> values = new ArrayList<>();
      value.stream().forEach(it -> it.toMap().values().forEach(values::addAll));
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

  class Object implements Value {
    private static final Map<String, Value> EMPTY = Collections.emptyMap();

    private Map<String, Value> hash = EMPTY;

    private final String name;

    protected Object(@Nonnull String name) {
      this.name = name;
    }

    protected Object() {
      this.name = null;
    }

    @Override public String name() {
      return name;
    }

    public Object put(String path, String value) {
      return put(path, Collections.singletonList(value));
    }

    public Object put(String path, FileUpload upload) {
      put(path, (name, scope) -> {
        Value existing = scope.get(name);
        if (existing == null) {
          scope.put(name, upload);
        } else {
          Array list;
          if (existing instanceof Array) {
            list = (Array) existing;
          } else {
            list = new Array(name).add(existing);
            scope.put(name, list);
          }
          list.add(upload);
        }
      });
      return this;
    }

    public Object put(String path, Collection<String> values) {
      put(path, (name, scope) -> {
        for (String value : values) {
          Value existing = scope.get(name);
          if (existing == null) {
            scope.put(name, new Simple(name, value));
          } else {
            Array list;
            if (existing instanceof Array) {
              list = (Array) existing;
            } else {
              list = new Array(name).add(existing);
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
      Object target = this;
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

    /*package*/ Object getOrCreateScope(String name) {
      return (Object) hash().computeIfAbsent(name, Object::new);
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
      throw new Err.BadRequest("Type mismatch: cannot convert object to string");
    }

    @Override public Iterator<Value> iterator() {
      return hash.values().iterator();
    }

    @Override public Map<String, List<String>> toMap() {
      Map<String, List<String>> result = new LinkedHashMap<>(hash.size());
      Set<Map.Entry<String, Value>> entries = hash.entrySet();
      String scope = name == null ? "" : name + ".";
      for (Map.Entry<String, Value> entry : entries) {
        Value value = entry.getValue();
        if (!value.isUpload()) {
          value.toMap().forEach((k, v) -> {
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

    @Override public Map<String, List<String>> toMap() {
      return Collections.emptyMap();
    }
  }

  class Simple implements Value {

    private final String name;

    private final String value;

    public Simple(String name, String value) {
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

    @Override public Map<String, List<String>> toMap() {
      return singletonMap(name, singletonList(value));
    }

    @Override public List<String> toList() {
      return singletonList(value);
    }

    @Override public Set<String> toSet() {
      return singleton(value);
    }
  }

  default long longValue() {
    try {
      return Long.parseLong(value());
    } catch (NumberFormatException x) {
      throw new Err.BadRequest("Type mismatch: cannot convert to number", x);
    }
  }

  default long longValue(long defaultValue) {
    try {
      return longValue();
    } catch (Err.Missing x) {
      return defaultValue;
    }
  }

  default int intValue() {
    try {
      return Integer.parseInt(value());
    } catch (NumberFormatException x) {
      throw new Err.BadRequest("Type mismatch: cannot convert to number", x);
    }
  }

  default int intValue(int defaultValue) {
    try {
      return intValue();
    } catch (Err.Missing x) {
      return defaultValue;
    }
  }

  default byte byteValue() {
    try {
      return Byte.parseByte(value());
    } catch (NumberFormatException x) {
      throw new Err.BadRequest("Type mismatch: cannot convert to number", x);
    }
  }

  default byte byteValue(byte defaultValue) {
    try {
      return byteValue();
    } catch (Err.Missing x) {
      return defaultValue;
    }
  }

  default float floatValue() {
    return Float.parseFloat(value());
  }

  default float floatValue(float defaultValue) {
    try {
      return floatValue();
    } catch (Err.Missing x) {
      return defaultValue;
    }
  }

  default double doubleValue() {
    return Double.parseDouble(value());
  }

  default double doubleValue(double defaultValue) {
    try {
      return doubleValue();
    } catch (Err.Missing x) {
      return defaultValue;
    }
  }

  default boolean booleanValue() {
    return Boolean.parseBoolean(value());
  }

  default boolean booleanValue(boolean defaultValue) {
    try {
      return booleanValue();
    } catch (Err.Missing x) {
      return defaultValue;
    }
  }

  @Nonnull default String value(@Nonnull String defaultValue) {
    try {
      return value();
    } catch (Err.Missing x) {
      return defaultValue;
    }
  }

  default boolean isArray() {
    return this instanceof Array;
  }

  default boolean isSimple() {
    return this instanceof Simple;
  }

  default boolean isObject() {
    return this instanceof Object;
  }

  default boolean isUpload() {
    return this instanceof FileUpload;
  }

  default boolean isMissing() {
    return this instanceof Missing;
  }

  default <T> T value(Throwing.Function<String, T> fn) {
    return fn.apply(value());
  }

  @Nonnull String value();

  default List<String> toList() {
    return Collections.emptyList();
  }

  default Set<String> toSet() {
    return Collections.emptySet();
  }

  default <T> List<T> toList(Throwing.Function<String, T> fn) {
    return toList().stream().map(fn).collect(Collectors.toList());
  }

  default <T> List<T> toList(Class<T> type) {
    return to(Reified.list(type));
  }

  default <T> Set<T> toSet(Throwing.Function<String, T> fn) {
    return toSet().stream().map(fn).collect(Collectors.toSet());
  }

  default <T extends Enum<T>> T toEnum(Throwing.Function<String, T> fn) {
    return toEnum(fn, String::toUpperCase);
  }

  default <T extends Enum<T>> T toEnum(Throwing.Function<String, T> fn,
      Function<String, String> caseFormatter) {
    return fn.apply(caseFormatter.apply(value()));
  }

  default Optional<String> toOptional() {
    try {
      return Optional.of(value());
    } catch (Err.Missing x) {
      return Optional.empty();
    }
  }

  default <T> Optional<T> toOptional(Throwing.Function<String, T> fn) {
    return toOptional().flatMap(v -> Optional.ofNullable(fn.apply(v)));
  }

  default <T> Optional<T> toOptional(Class<T> type) {
    return to(Reified.optional(type));
  }

  default FileUpload fileUpload() {
    throw new Err.BadRequest("Type mismatch: not a file upload");
  }

  /* ***********************************************************************************************
   * Node methods
   * ***********************************************************************************************
   */
  @Nonnull Value get(@Nonnull int index);

  @Nonnull Value get(@Nonnull String name);

  default int size() {
    return 0;
  }

  default <T> T to(Class<T> type) {
    ValueInjector injector = new ValueInjector();
    return injector.inject(this, type, type);
  }

  default <T> T to(Reified<T> type) {
    ValueInjector injector = new ValueInjector();
    return injector.inject(this, type.getType(), type.getRawType());
  }

  default @Override Iterator<Value> iterator() {
    return Collections.emptyIterator();
  }

  String name();

  Map<String, List<String>> toMap();

  /* ***********************************************************************************************
   * Factory methods
   * ***********************************************************************************************
   */
  static Value missing(String name) {
    return new Missing(name);
  }

  static Value value(String name, String value) {
    return new Simple(name, value);
  }

  static Array array(@Nonnull String name) {
    return new Array(name);
  }

  static Value create(@Nonnull String name, @Nullable List<String> values) {
    if (values == null || values.size() == 0) {
      return missing(name);
    }
    if (values.size() == 1) {
      return value(name, values.get(0));
    }
    Array array = array(name);
    for (String value : values) {
      array.add(value);
    }
    return array;
  }

  static Value.Object object(@Nonnull String name) {
    return new Object(name);
  }

  static Value.Object headers() {
    return new Object(null);
  }

  static Value.Object path(Map<String, String> params) {
    Value.Object path = new Object(null);
    params.forEach(path::put);
    return path;
  }

  static QueryString queryString(@Nonnull String queryString) {
    return UrlParser.queryString(queryString);
  }
}
