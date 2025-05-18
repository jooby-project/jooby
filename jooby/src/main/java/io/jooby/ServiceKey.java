/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.lang.reflect.Type;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.internal.reflect.$Types;

/**
 * Utility class to access application services.
 *
 * @param <T> Service type.
 */
public final class ServiceKey<T> {
  private final Type type;
  private final Class<T> rawType;

  private final int hashCode;

  private final String name;

  private ServiceKey(Type type, Class<T> rawType, String name) {
    this.type = type;
    this.rawType = rawType;
    this.name = name;
    this.hashCode = Objects.hash(type, name);
  }

  /**
   * Resource type.
   *
   * @return Resource type.
   */
  public @NonNull Type getType() {
    return type;
  }

  public @NonNull Class<T> getRawType() {
    return rawType;
  }

  /**
   * Resource name or <code>null</code>.
   *
   * @return Resource name or <code>null</code>.
   */
  public @Nullable String getName() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServiceKey<?> key) {
      return this.type.equals(key.type) && Objects.equals(this.name, key.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    var typeName = $Types.typeToString(type);
    if (name == null) {
      return typeName;
    }
    return typeName + "(" + name + ")";
  }

  /**
   * Creates a resource key.
   *
   * @param type Resource type.
   * @param <T> Type.
   * @return A new resource key.
   */
  public static @NonNull <T> ServiceKey<T> key(@NonNull Class<T> type) {
    return new ServiceKey<>(type, type, null);
  }

  /**
   * Creates a named resource key.
   *
   * @param type Resource type.
   * @param name Resource name.
   * @param <T> Type.
   * @return A new resource key.
   */
  public static @NonNull <T> ServiceKey<T> key(@NonNull Class<T> type, @NonNull String name) {
    return new ServiceKey<>(type, type, name);
  }

  @SuppressWarnings("unchecked")
  public static @NonNull <T> ServiceKey<T> key(@NonNull Reified<T> type, @NonNull String name) {
    return new ServiceKey<>(type.getType(), (Class<T>) type.getRawType(), name);
  }

  @SuppressWarnings("unchecked")
  public static @NonNull <T> ServiceKey<T> key(@NonNull Reified<T> type) {
    return new ServiceKey<>(type.getType(), (Class<T>) type.getRawType(), null);
  }
}
