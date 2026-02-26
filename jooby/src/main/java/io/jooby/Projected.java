/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.*;
import java.util.function.Consumer;

/**
 * A wrapper for a value and its associated {@link Projection}.
 *
 * @param <T> The value type.
 * @author edgar
 * @since 4.0.0
 */
public class Projected<T> {
  private final T value;
  private final Projection<T> projection;

  private Projected(T value, Projection<T> projection) {
    this.value = value;
    this.projection = projection;
  }

  @SuppressWarnings("unchecked")
  public static <T> Projected<T> wrap(T value) {
    return new Projected<T>(value, Projection.of(computeProjectionType(value)));
  }

  @SuppressWarnings("rawtypes")
  private static Class computeProjectionType(Object value) {
    return switch (value) {
      case Set<?> col -> col.isEmpty() ? Object.class : col.iterator().next().getClass();
      case Collection<?> col -> col.isEmpty() ? Object.class : col.iterator().next().getClass();
      case Optional<?> optional -> optional.isEmpty() ? Object.class : optional.get().getClass();
      default -> value.getClass();
    };
  }

  public static <T> Projected<T> wrap(T value, Projection<T> projection) {
    return new Projected<>(value, projection);
  }

  public T getValue() {
    return value;
  }

  public Projection<T> getProjection() {
    return projection;
  }

  public Projected<T> include(String... paths) {
    projection.include(paths);
    return this;
  }

  @SafeVarargs
  public final Projected<T> include(Projection.Property<T, ?>... props) {
    projection.include(props);
    return this;
  }

  public <R> Projected<T> include(Projection.Property<T, R> prop, Consumer<Projection<R>> child) {
    projection.include(prop, child);
    return this;
  }

  @Override
  public String toString() {
    return projection.toString();
  }
}
