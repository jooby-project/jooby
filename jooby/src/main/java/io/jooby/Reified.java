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

import io.jooby.internal.reflect.$Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a generic type {@code T}. Java doesn't yet provide a way to
 * represent generic types, so this class does. Forces clients to create a
 * subclass of this class which enables retrieval the type information even at
 * runtime.
 *
 * <p>For example, to create a type literal for {@code List<String>}, you can
 * create an empty anonymous inner class:
 *
 * <p>
 * {@code Reified<List<String>> list = new Reified<List<String>>() {};}
 *
 * <p>This syntax cannot be used to create type literals that have wildcard
 * parameters, such as {@code Class<?>} or {@code List<? extends CharSequence>}.
 *
 * @author Bob Lee
 * @author Sven Mawson
 * @author Jesse Wilson
 */
public class Reified<T> {
  public static final Reified<Integer> INT = new Reified<>(Integer.class);

  final Class<? super T> rawType;
  final Type type;
  final int hashCode;

  /**
   * Constructs a new type literal. Derives represented class from type
   * parameter.
   *
   * <p>Clients create an empty anonymous subclass. Doing so embeds the type
   * parameter in the anonymous class's type hierarchy so we can reconstitute it
   * at runtime despite erasure.
   */
  @SuppressWarnings("unchecked")
  protected Reified() {
    this.type = getSuperclassTypeParameter(getClass());
    this.rawType = (Class<? super T>) $Types.getRawType(type);
    this.hashCode = type.hashCode();
  }

  /**
   * Unsafe. Constructs a type literal manually.
   */
  @SuppressWarnings("unchecked")
  private Reified(Type type) {
    this.type = $Types.canonicalize(type);
    this.rawType = (Class<? super T>) $Types.getRawType(this.type);
    this.hashCode = this.type.hashCode();
  }

  private Reified(Class type) {
    this.type = type;
    this.rawType = type;
    this.hashCode = type.hashCode();
  }

  /**
   * Returns the type from super class's type parameter in {@link $Types#canonicalize
   * canonical form}.
   */
  static Type getSuperclassTypeParameter(Class<?> subclass) {
    Type superclass = subclass.getGenericSuperclass();
    if (superclass instanceof Class) {
      throw new RuntimeException("Missing type parameter.");
    }
    ParameterizedType parameterized = (ParameterizedType) superclass;
    return $Types.canonicalize(parameterized.getActualTypeArguments()[0]);
  }

  /**
   * Returns the raw (non-generic) type for this type.
   */
  public final Class<? super T> getRawType() {
    return rawType;
  }

  /**
   * Gets underlying {@code Type} instance.
   */
  public final Type getType() {
    return type;
  }

  @Override public final int hashCode() {
    return this.hashCode;
  }

  @Override public final boolean equals(Object o) {
    return o instanceof Reified<?>
        && $Types.equals(type, ((Reified<?>) o).type);
  }

  @Override public final String toString() {
    return $Types.typeToString(type);
  }

  /**
   * Gets type literal for the given {@code Type} instance.
   */
  public static Reified<?> get(Type type) {
    return new Reified<>(type);
  }

  public static Class<?> rawType(Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    }
    return new Reified<>(type).rawType;
  }

  /**
   * Gets type literal for the given {@code Class} instance.
   */
  public static <T> Reified<T> get(Class<T> type) {
    if (type == int.class || type == Integer.class) {
      return (Reified<T>) INT;
    }
    return new Reified<>(type);
  }

  public static <T> Reified<List<T>> list(Type type) {
    return (Reified<List<T>>) getParameterized(List.class, type);
  }

  public static <T> Reified<Set<T>> set(Type type) {
    return (Reified<Set<T>>) getParameterized(Set.class, type);
  }

  public static <T> Reified<Optional<T>> optional(Type type) {
    return (Reified<Optional<T>>) getParameterized(Optional.class, type);
  }

  public static <K, V> Reified<Map<K, V>> map(Type key, Type value) {
    return (Reified<Map<K, V>>) getParameterized(Map.class, key, value);
  }

  public static <T> Reified<CompletableFuture<T>> completableFuture(Type type) {
    return (Reified<CompletableFuture<T>>) getParameterized(CompletableFuture.class, type);
  }

  /**
   * Gets type literal for the parameterized type represented by applying {@code typeArguments} to
   * {@code rawType}.
   */
  public static Reified<?> getParameterized(Type rawType, Type... typeArguments) {
    return new Reified<>($Types.newParameterizedTypeWithOwner(null, rawType, typeArguments));
  }

  /**
   * Gets type literal for the array type whose elements are all instances of {@code componentType}.
   */
  public static Reified<?> getArray(Type componentType) {
    return new Reified<>($Types.arrayOf(componentType));
  }
}
