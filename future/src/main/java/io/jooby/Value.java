package io.jooby;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public interface Value {

  class Array implements Value {
    private final String name;

    private final List<Value> value = new ArrayList<>(5);

    public Array(String name) {
      this.name = name;
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
        return new Missing(Integer.toString(index));
      }
    }

    @Override public Value get(@Nonnull String name) {
      return new Missing(name);
    }

    @Override public int size() {
      return value.size();
    }

    @Override public String value() {
      throw new Err.TypeMismatch("cannot convert array to string");
    }

    @Override public String toString() {
      return value.toString();
    }

    @Override public Map<String, List<String>> toMap() {
      List<String> values = new ArrayList<>();
      value.stream().forEach(it -> it.toMap().values().forEach(values::addAll));
      return Collections.singletonMap(name, values);
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

    public Object put(String name, String value) {
      Map<String, Value> hash = hash();
      Value existing = hash.get(name);
      if (existing == null) {
        hash.put(name, new Simple(name, value));
      } else {
        Array list;
        if (existing instanceof Array) {
          list = (Array) existing;
        } else {
          list = new Array(name).add(existing);
          hash.put(name, list);
        }
        list.add(value);
      }
      return this;
    }

    private Map<String, Value> hash() {
      if (hash == EMPTY) {
        hash = new LinkedHashMap<>();
      }
      return hash;
    }

    public Object getOrCreateScope(String name) {
      return (Object) hash().computeIfAbsent(name, Object::new);
    }

    public Value get(@Nonnull String name) {
      Value value = hash.get(name);
      if (value == null) {
        return new Missing(name);
      }
      return value;
    }

    @Override public Value get(@Nonnull int index) {
      return get(Integer.toString(index));
    }

    public int size() {
      return hash.size();
    }

    @Override public String value() {
      throw new Err.TypeMismatch("cannot convert object to string");
    }

    @Override public Map<String, List<String>> toMap() {
      Map<String, List<String>> result = new LinkedHashMap<>(hash.size());
      Set<Map.Entry<String, Value>> entries = hash.entrySet();
      String scope = name == null ? "" : name + ".";
      for (Map.Entry<String, Value> entry : entries) {
        Value value = entry.getValue();
        value.toMap().forEach((k, v) -> {
          result.put(scope + k, v);
        });
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

    @Override public Value get(@Nonnull String name) {
      return this.name.equals(name) ? this : new Missing(name);
    }

    @Override public Value get(@Nonnull int index) {
      return get(Integer.toString(index));
    }

    @Override public String value() {
      throw new Err.Missing("[" + name + "]");
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

    @Override public Value get(@Nonnull int index) {
      return get(Integer.toString(index));
    }

    @Override public Value get(@Nonnull String name) {
      return new Missing(name);
    }

    @Override public String value() {
      return value;
    }

    @Override public String toString() {
      return value;
    }

    @Override public Map<String, List<String>> toMap() {
      return singletonMap(name, singletonList(value));
    }
  }

  default long longValue() {
    return Long.parseLong(value());
  }

  default long longValue(long defaultValue) {
    try {
      return longValue();
    } catch (Err.Missing x) {
      return defaultValue;
    }
  }

  default int intValue() {
    return Integer.parseInt(value());
  }

  default int intValue(int defaultValue) {
    try {
      return intValue();
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

  default char charValue() {
    return value().charAt(0);
  }

  default char charValue(char defaultValue) {
    try {
      return charValue();
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

  default boolean isObject() {
    return this instanceof Object;
  }

  @Nonnull String value();

  /* ***********************************************************************************************
   * Node methods
   * ***********************************************************************************************
   */
  @Nonnull Value get(@Nonnull int index);

  @Nonnull Value get(@Nonnull String name);

  default int size() {
    return 0;
  }

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

  static Object object(@Nonnull String name) {
    return new Object(name);
  }

  static QueryString queryString(@Nonnull String queryString) {
    return UrlParser.queryString(queryString);
  }
}
