/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.Value;

import javax.annotation.Nonnull;
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
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class HashValue implements Value, Multipart {
  private static final Map<String, Value> EMPTY = Collections.emptyMap();

  private Context ctx;

  private Map<String, Value> hash = EMPTY;

  private final String name;

  public HashValue(Context ctx, String name) {
    this.ctx = ctx;
    this.name = name;
  }

  protected HashValue(Context ctx) {
    this.ctx = ctx;
    this.name = null;
  }

  @Override public String name() {
    return name;
  }

  public Formdata put(String path, String value) {
    return put(path, Collections.singletonList(value));
  }

  public HashValue put(String path, Value upload) {
    put(path, (name, scope) -> {
      Value existing = scope.get(name);
      if (existing == null) {
        scope.put(name, upload);
      } else {
        ArrayValue list;
        if (existing instanceof ArrayValue) {
          list = (ArrayValue) existing;
        } else {
          list = new ArrayValue(ctx, name).add(existing);
          scope.put(name, list);
        }
        list.add(upload);
      }
    });
    return this;
  }

  public HashValue put(String path, Collection<String> values) {
    put(path, (name, scope) -> {
      for (String value : values) {
        Value existing = scope.get(name);
        if (existing == null) {
          scope.put(name, new SingleValue(ctx, name, value));
        } else {
          ArrayValue list;
          if (existing instanceof ArrayValue) {
            list = (ArrayValue) existing;
          } else {
            list = new ArrayValue(ctx, name).add(existing);
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
    HashValue target = this;
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
    if (hash instanceof TreeMap) {
      return;
    }
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

  /*package*/ HashValue getOrCreateScope(String name) {
    return (HashValue) hash().computeIfAbsent(name, k -> new HashValue(ctx, k));
  }

  public Value get(@Nonnull String name) {
    Value value = hash.get(name);
    if (value == null) {
      return new MissingValue(scope(name));
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
    StringJoiner joiner = new StringJoiner("&");
    hash.forEach((k, v) -> {
      Iterator<Value> it = v.iterator();
      while (it.hasNext()) {
        Value value = it.next();
        String str = value instanceof FileUpload
            ? ((FileUpload) value).getFileName()
            : value.toString();
        joiner.add(k + "=" + str);
      }
    });
    return joiner.toString();
  }

  @Override public Iterator<Value> iterator() {
    return hash.values().iterator();
  }

  @Nonnull @Override public <T> List<T> toList(@Nonnull Class<T> type) {
    return toCollection(type, new ArrayList<>());
  }

  @Nonnull @Override public <T> Set<T> toSet(@Nonnull Class<T> type) {
    return toCollection(type, new LinkedHashSet<>());
  }

  @Nonnull @Override public <T> Optional<T> toOptional(@Nonnull Class<T> type) {
    if (hash.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(to(type));
  }

  @Nonnull @Override public <T> T to(@Nonnull Class<T> type) {
    return ctx.convert(this, type);
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

  public HashValue put(Map<String, Collection<String>> headers) {
    for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
    return this;
  }

  private <T, C extends Collection<T>> C toCollection(@Nonnull Class<T> type, C collection) {
    if (hash instanceof TreeMap) {
      // indexes access, treat like a list
      Collection<Value> values = hash.values();
      for (Value value : values) {
        collection.add(value.to(type));
      }
    } else {
      collection.add(to(type));
    }
    return collection;
  }
}
