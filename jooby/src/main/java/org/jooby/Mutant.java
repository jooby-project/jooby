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

import com.google.inject.TypeLiteral;

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
 *   Optional<Integer> value = request.param("some").getOptional(Integer.class);

 *   // list param
 *   List<String> values = request.param("some").getList(String.class);
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
  boolean booleanValue();

  /**
   * @return Get a byte when possible.
   */
  byte byteValue();

  /**
   * @return Get a short when possible.
   */
  short shortValue();

  /**
   * @return Get an integer when possible.
   */
  int intValue();

  /**
   * @return Get a long when possible.
   */
  long longValue();

  /**
   * @return Get a string when possible.
   */
  String stringValue();

  /**
   * @return Get a float when possible.
   */
  float floatValue();

  /**
   * @return Get a double when possible.
   */
  double doubleValue();

  /**
   * @return Get an enum when possible.
   * @param type The enum type.
   */
  <T extends Enum<T>> T enumValue(@Nonnull Class<T> type);

  /**
   * @return Get list of values when possible.
   * @param type The element type.
   */
  <T> List<T> toList(@Nonnull Class<T> type);

  /**
   * @return Get set of values when possible.
   * @param type The element type.
   */
  <T> Set<T> toSet(@Nonnull Class<T> type);

  /**
   * @return Get sorted set of values when possible.
   * @param type The element type.
   */
  <T extends Comparable<T>> SortedSet<T> toSortedSet(@Nonnull Class<T> type);

  /**
   * @return Get an optional value when possible.
   * @param type The optional type.
   */
  <T> Optional<T> toOptional(@Nonnull Class<T> type);

  /**
   * Get a value using one of the existing and specific converters or an arbitrary type that has:
   *
   * <ol>
   * <li>A constructor that accepts a {@link String}</li>
   * <li>A static method <code>valueOf</code> that takes a single {@link String} parameter.</li>
   * <li>A static method <code>fromString</code> that takes a single {@link String} parameter.</li>
   * </ol>
   *
   * @return Get a value when possible.
   * @param type The type to convert to.
   */
  default <T> T to(final Class<T> type) {
    return to(TypeLiteral.get(type));
  }

  /**
   * Get a value using one of the existing and specific converters or an arbitrary type that has:
   *
   * <ol>
   * <li>A constructor that accepts a {@link String}</li>
   * <li>A static method <code>valueOf</code> that takes a single {@link String} parameter.</li>
   * <li>A static method <code>fromString</code> that takes a single {@link String} parameter.</li>
   * </ol>
   *
   * @return Get a value when possible.
   * @param type The type to convert to.
   */
  <T> T to(TypeLiteral<T> type);

  /**
   * @return True if the variant contains a value.
   */
  boolean isPresent();
}
