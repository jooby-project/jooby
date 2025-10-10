/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.reflect.$Types;

/**
 * Represents a generic type {@code T}. Java doesn't yet provide a way to represent generic types,
 * so this class does. Forces clients to create a subclass of this class which enables retrieval the
 * type information even at runtime.
 *
 * <p>For example, to create a type literal for {@code List<String>}, you can create an empty
 * anonymous inner class:
 *
 * <p>{@code Reified<List<String>> list = new Reified<List<String>>() {};}
 *
 * <p>This syntax cannot be used to create type literals that have wildcard parameters, such as
 * {@code Class<?>} or {@code List<? extends CharSequence>}.
 *
 * @param <T> Target type.
 * @author Bob Lee
 * @author Sven Mawson
 * @author Jesse Wilson
 */
public class Reified<T> {
  @SuppressWarnings("rawtypes")
  private final Class rawType;

  private final Type type;
  private final int hashCode;

  /**
   * Constructs a new type literal. Derives represented class from type parameter.
   *
   * <p>Clients create an empty anonymous subclass. Doing so embeds the type parameter in the
   * anonymous class's type hierarchy so we can reconstitute it at runtime despite erasure.
   */
  @SuppressWarnings("unchecked")
  public Reified() {
    this.type = getSuperclassTypeParameter(getClass());
    this.rawType = (Class<? super T>) $Types.getRawType(type);
    this.hashCode = type.hashCode();
  }

  /**
   * Unsafe. Constructs a type literal manually.
   *
   * @param type Generic type.
   */
  private Reified(Type type) {
    this.type = $Types.canonicalize(type);
    this.rawType = $Types.getRawType(this.type);
    this.hashCode = this.type.hashCode();
  }

  /**
   * Unsafe. Constructs a type literal manually.
   *
   * @param type Generic type.
   */
  @SuppressWarnings("rawtypes")
  private Reified(Class type) {
    this.type = type;
    this.rawType = type;
    this.hashCode = type.hashCode();
  }

  /**
   * Returns the type from super class's type parameter in {@link $Types#canonicalize canonical
   * form}.
   *
   * @param subclass Subclass.
   * @return Type.
   */
  private static Type getSuperclassTypeParameter(Class<?> subclass) {
    Type superclass = subclass.getGenericSuperclass();
    if (superclass instanceof Class) {
      throw new RuntimeException("Missing type parameter.");
    }
    ParameterizedType parameterized = (ParameterizedType) superclass;
    return $Types.canonicalize(parameterized.getActualTypeArguments()[0]);
  }

  /**
   * Returns the raw (non-generic) type for this type.
   *
   * @return Returns the raw (non-generic) type for this type.
   */
  public final Class<? super T> getRawType() {
    return rawType;
  }

  /**
   * Gets underlying {@code Type} instance.
   *
   * @return Gets underlying {@code Type} instance.
   */
  public final Type getType() {
    return type;
  }

  @Override
  public final int hashCode() {
    return this.hashCode;
  }

  @Override
  public final boolean equals(Object o) {
    return o instanceof Reified<?> && $Types.equals(type, ((Reified<?>) o).type);
  }

  @Override
  public final String toString() {
    return $Types.typeToString(type);
  }

  /**
   * Gets type literal for the given {@code Type} instance.
   *
   * @param type Source type.
   * @return Gets type literal for the given {@code Type} instance.
   */
  public static Reified<?> get(@NonNull Type type) {
    return new Reified<>(type);
  }

  /**
   * Get raw type (class) from given type.
   *
   * @param type Type.
   * @return Raw type.
   */
  public static Class<?> rawType(@NonNull Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    }
    return new Reified<>(type).rawType;
  }

  /**
   * Gets type literal for the given {@code Class} instance.
   *
   * @param type Java type.
   * @param <T> Generic type.
   * @return Gets type literal for the given {@code Class} instance.
   */
  public static <T> Reified<T> get(@NonNull Class<T> type) {
    return new Reified<>(type);
  }

  /**
   * Creates a {@link List} type literal.
   *
   * @param type Item type.
   * @param <T> Item type.
   * @return A {@link List} type literal.
   */
  public static <T> Reified<List<T>> list(@NonNull Type type) {
    return getParameterized(List.class, type);
  }

  /**
   * Creates a {@link List} type literal.
   *
   * @param type Item type.
   * @param <T> Item type.
   * @return A {@link List} type literal.
   */
  public static <T> Reified<List<T>> list(@NonNull Reified<T> type) {
    return getParameterized(List.class, type.getType());
  }

  /**
   * Creates a {@link Set} type literal.
   *
   * @param type Item type.
   * @param <T> Item type.
   * @return A {@link Set} type literal.
   */
  public static <T> Reified<Set<T>> set(@NonNull Type type) {
    return getParameterized(Set.class, type);
  }

  /**
   * Creates a {@link Set} type literal.
   *
   * @param type Item type.
   * @param <T> Item type.
   * @return A {@link Set} type literal.
   */
  public static <T> Reified<Set<T>> set(@NonNull Reified<T> type) {
    return getParameterized(Set.class, type.getType());
  }

  /**
   * Creates an {@link Optional} type literal.
   *
   * @param type Item type.
   * @param <T> Item type.
   * @return A {@link Optional} type literal.
   */
  public static <T> Reified<Optional<T>> optional(@NonNull Type type) {
    return getParameterized(Optional.class, type);
  }

  /**
   * Creates an {@link Optional} type literal.
   *
   * @param type Item type.
   * @param <T> Item type.
   * @return A {@link Optional} type literal.
   */
  public static <T> Reified<Optional<T>> optional(@NonNull Reified<T> type) {
    return getParameterized(Optional.class, type.getType());
  }

  /**
   * Creates an {@link Map} type literal.
   *
   * @param key Key type.
   * @param value Value type.
   * @param <K> Key type.
   * @param <V> Key type.
   * @return A {@link Map} type literal.
   */
  public static <K, V> Reified<Map<K, V>> map(@NonNull Type key, @NonNull Type value) {
    return getParameterized(Map.class, key, value);
  }

  /**
   * Creates an {@link Map} type literal.
   *
   * @param key Key type.
   * @param value Value type.
   * @param <K> Key type.
   * @param <V> Key type.
   * @return A {@link Map} type literal.
   */
  public static <K, V> Reified<Map<K, V>> map(@NonNull Reified<K> key, @NonNull Reified<V> value) {
    return getParameterized(Map.class, key.getType(), value.getType());
  }

  /**
   * Creates a {@link CompletableFuture} type literal.
   *
   * @param type Item type.
   * @param <T> Item type.
   * @return A {@link CompletableFuture} type literal.
   */
  public static <T> Reified<CompletableFuture<T>> completableFuture(@NonNull Type type) {
    return getParameterized(CompletableFuture.class, type);
  }

  /**
   * Gets type literal for the parameterized type represented by applying {@code typeArguments} to
   * {@code rawType}.
   *
   * @param rawType Raw type.
   * @param typeArguments Parameter types.
   * @return Gets type literal for the parameterized type represented by applying {@code
   *     typeArguments} to {@code rawType}.
   */
  public static <T> Reified<T> getParameterized(
      @NonNull Type rawType, @NonNull Type... typeArguments) {
    return new Reified<>($Types.newParameterizedTypeWithOwner(null, rawType, typeArguments));
  }

  /**
   * Gets type literal for the parameterized type represented by applying {@code typeArguments} to
   * {@code rawType}.
   *
   * @param rawType Raw type.
   * @param argument Parameter types.
   * @return Gets type literal for the parameterized type represented by applying {@code
   *     typeArguments} to {@code rawType}.
   */
  public static <T> Reified<T> getParameterized(
      @NonNull Type rawType, @NonNull Reified<?> argument) {
    return new Reified<>($Types.newParameterizedTypeWithOwner(null, rawType, argument.getType()));
  }
}
