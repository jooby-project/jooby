/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nonnull;

import com.google.common.primitives.Primitives;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

/**
 * <p>
 * A type safe {@link Mutant} useful for reading parameters and headers. It let you retrieve a
 * HTTP value: <code>param</code> or <code>header</code> in a type safe manner.
 * </p>
 *
 * <pre>
 *   // int param
 *   int value = request.param("some").getInt();
 *
 *   // optional int param
 *   Optional{@literal <}Integer{@literal >} value = request.param("some").getOptional(Integer.class);

 *   // list param
 *   List{@literal <}String{@literal >} values = request.param("some").getList(String.class);
 *
 *   // file upload
 *   Upload upload = request.param("file").get(Upload.class);
 * </pre>
 *
 * @author edgar
 * @since 0.1.0
 * @see Request#param(String)
 * @see Request#header(String)
 */
public interface Mutant {

  /**
   * @return Get a boolean when possible.
   */
  default boolean booleanValue() {
    return to(boolean.class);
  }

  /**
   * @return Get a byte when possible.
   */
  default byte byteValue() {
    return to(byte.class);
  }

  /**
   * @return Get a short when possible.
   */
  default short shortValue() {
    return to(short.class);
  }

  /**
   * @return Get an integer when possible.
   */
  default int intValue() {
    return to(int.class);
  }

  /**
   * @return Get a long when possible.
   */
  default long longValue() {
    return to(long.class);
  }

  /**
   * @return Get a string when possible.
   */
  default String value() {
    return to(String.class);
  }

  /**
   * @return Get a float when possible.
   */
  default float floatValue() {
    return to(float.class);
  }

  /**
   * @return Get a double when possible.
   */
  default double doubleValue() {
    return to(double.class);
  }

  /**
   * @param type The enum type.
   * @param <T> Enum type.
   * @return Get an enum when possible.
   */
  default <T extends Enum<T>> T toEnum(@Nonnull final Class<T> type) {
    return to(type);
  }

  /**
   * @param type The element type.
   * @param <T> List type.
   * @return Get list of values when possible.
   */
  @SuppressWarnings("unchecked")
  default <T> List<T> toList(@Nonnull final Class<T> type) {
    return (List<T>) to(TypeLiteral.get(Types.listOf(Primitives.wrap(type))));
  }

  /**
   * @param type The element type.
   * @param <T> Set type.
   * @return Get set of values when possible.
   */
  @SuppressWarnings("unchecked")
  default <T> Set<T> toSet(@Nonnull final Class<T> type) {
    return (Set<T>) to(TypeLiteral.get(Types.setOf(Primitives.wrap(type))));
  }

  /**
   * @param type The element type.
   * @param <T> Set type.
   * @return Get sorted set of values when possible.
   */
  @SuppressWarnings("unchecked")
  default <T extends Comparable<T>> SortedSet<T> toSortedSet(@Nonnull final Class<T> type) {
    return (SortedSet<T>) to(TypeLiteral.get(
        Types.newParameterizedType(SortedSet.class, Primitives.wrap(type))
        ));
  }

  /**
   * @param type The optional type.
   * @param <T> Optional type.
   * @return Get an optional value when possible.
   */
  @SuppressWarnings("unchecked")
  default <T> Optional<T> toOptional(@Nonnull final Class<T> type) {
    return (Optional<T>) to(TypeLiteral.get(
        Types.newParameterizedType(Optional.class, Primitives.wrap(type))
        ));
  }

  /**
   * Convert a raw value to the given type.
   *
   * @param type The type to convert to.
   * @param <T> Target type.
   * @return Get a value when possible.
   */
  default <T> T to(final Class<T> type) {
    return to(TypeLiteral.get(type));
  }

  /**
   * Convert a raw value to the given type.
   *
   * @param type The type to convert to.
   * @param <T> Target type.
   * @return Get a value when possible.
   */
  <T> T to(TypeLiteral<T> type);

  /**
   * @return True if the variant contains a value.
   */
  boolean isPresent();
}
