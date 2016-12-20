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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.primitives.Primitives;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

/**
 * <p>
 * A type safe {@link Mutant} useful for reading parameters/headers/session attributes, etc..
 * </p>
 *
 * <pre>
 *   // str param
 *   String value = request.param("str").value();
 *
 *   // optional str
 *   String value = request.param("str").value("defs");
 *
 *   // int param
 *   int value = request.param("some").intValue();
 *
 *   // optional int param
 *   Optional{@literal <}Integer{@literal >} value = request.param("some").toOptional(Integer.class);

 *   // list param
 *   List{@literal <}String{@literal >} values = request.param("some").toList(String.class);
 *
 *   // file upload
 *   Upload upload = request.param("file").to(Upload.class);
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
   * @param value Default value to use.
   * @return Get a boolean.
   */
  default boolean booleanValue(final boolean value) {
    return toOptional(Boolean.class).orElse(value);
  }

  /**
   * @return Get a byte when possible.
   */
  default byte byteValue() {
    return to(byte.class);
  }

  /**
   * @param value Default value to use.
   * @return Get a byte.
   */
  default byte byteValue(final byte value) {
    return toOptional(Byte.class).orElse(value);
  }

  /**
   * @return Get a byte when possible.
   */
  default char charValue() {
    return to(char.class);
  }

  /**
   * @param value Default value to use.
   * @return Get a char.
   */
  default char charValue(final char value) {
    return toOptional(Character.class).orElse(value);
  }

  /**
   * @return Get a short when possible.
   */
  default short shortValue() {
    return to(short.class);
  }

  /**
   * @param value Default value to use.
   * @return Get a short value.
   */
  default short shortValue(final short value) {
    return toOptional(Short.class).orElse(value);
  }

  /**
   * @return Get an integer when possible.
   */
  default int intValue() {
    return to(int.class);
  }

  /**
   * @param value Default value to use.
   * @return Get an integer.
   */
  default int intValue(final int value) {
    return toOptional(Integer.class).orElse(value);
  }

  /**
   * @return Get a long when possible.
   */
  default long longValue() {
    return to(long.class);
  }

  /**
   * @param value Default value to use.
   * @return Get a long.
   */
  default long longValue(final long value) {
    return toOptional(Long.class).orElse(value);
  }

  /**
   * @return Get a string when possible.
   */
  default String value() {
    return to(String.class);
  }

  /**
   * @param value Default value to use.
   * @return Get a string.
   */
  default String value(final String value) {
    return toOptional().orElse(value);
  }

  /**
   * @return Get a float when possible.
   */
  default float floatValue() {
    return to(float.class);
  }

  /**
   * @param value Default value to use.
   * @return Get a float.
   */
  default float floatValue(final float value) {
    return toOptional(Float.class).orElse(value);
  }

  /**
   * @return Get a double when possible.
   */
  default double doubleValue() {
    return to(double.class);
  }

  /**
   * @param value Default value to use.
   * @return Get a double.
   */
  default double doubleValue(final double value) {
    return toOptional(Double.class).orElse(value);
  }

  /**
   * @param type The enum type.
   * @param <T> Enum type.
   * @return Get an enum when possible.
   */
  default <T extends Enum<T>> T toEnum(final Class<T> type) {
    return to(type);
  }

  /**
   * @param value Default value to use.
   * @param <T> Enum type.
   * @return Get an enum.
   */
  @SuppressWarnings("unchecked")
  default <T extends Enum<T>> T toEnum(final T value) {
    Optional<T> optional = (Optional<T>) toOptional(value.getClass());
    return optional.orElse(value);
  }

  /**
   * @param type The element type.
   * @param <T> List type.
   * @return Get list of values when possible.
   */
  @SuppressWarnings("unchecked")
  default <T> List<T> toList(final Class<T> type) {
    return (List<T>) to(TypeLiteral.get(Types.listOf(Primitives.wrap(type))));
  }

  /**
   * @return Get list of values when possible.
   */
  default List<String> toList() {
    return toList(String.class);
  }

  /**
   * @return Get set of values when possible.
   */
  default Set<String> toSet() {
    return toSet(String.class);
  }

  /**
   * @param type The element type.
   * @param <T> Set type.
   * @return Get set of values when possible.
   */
  @SuppressWarnings("unchecked")
  default <T> Set<T> toSet(final Class<T> type) {
    return (Set<T>) to(TypeLiteral.get(Types.setOf(Primitives.wrap(type))));
  }

  /**
   * @return Get sorted set of values when possible.
   */
  default SortedSet<String> toSortedSet() {
    return toSortedSet(String.class);
  }

  /**
   * @param type The element type.
   * @param <T> Set type.
   * @return Get sorted set of values when possible.
   */
  @SuppressWarnings("unchecked")
  default <T extends Comparable<T>> SortedSet<T> toSortedSet(final Class<T> type) {
    return (SortedSet<T>) to(TypeLiteral.get(
        Types.newParameterizedType(SortedSet.class, Primitives.wrap(type))
        ));
  }

  /**
   * @return An optional string value.
   */
  default Optional<String> toOptional() {
    return toOptional(String.class);
  }

  /**
   * @param type The optional type.
   * @param <T> Optional type.
   * @return Get an optional value when possible.
   */
  @SuppressWarnings("unchecked")
  default <T> Optional<T> toOptional(final Class<T> type) {
    return (Optional<T>) to(TypeLiteral.get(
        Types.newParameterizedType(Optional.class, Primitives.wrap(type))
        ));
  }

  /**
   * Convert a form field to file {@link Upload}.
   *
   * @return A file {@link Upload}.
   */
  default Upload toUpload() {
    return to(Upload.class);
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
   * Convert a raw value to the given type. This method will temporary set {@link MediaType} before
   * parsing a value, useful if a form field from a HTTP POST was send as json (or any other data).
   *
   * @param type The type to convert to.
   * @param mtype A media type to hint a parser.
   * @param <T> Target type.
   * @return Get a value when possible.
   */
  default <T> T to(final Class<T> type, final String mtype) {
    return to(type, MediaType.valueOf(mtype));
  }

  /**
   * Convert a raw value to the given type. This method will temporary set {@link MediaType} before
   * parsing a value, useful if a form field from a HTTP POST was send as json (or any other data).
   *
   * @param type The type to convert to.
   * @param mtype A media type to hint a parser.
   * @param <T> Target type.
   * @return Get a value when possible.
   */
  default <T> T to(final Class<T> type, final MediaType mtype) {
    return to(TypeLiteral.get(type), mtype);
  }

  /**
   * Convert a raw value to the given type. This method will temporary set {@link MediaType} before
   * parsing a value, useful if a form field from a HTTP POST was send as json (or any other data).
   *
   * @param type The type to convert to.
   * @param mtype A media type to hint a parser.
   * @param <T> Target type.
   * @return Get a value when possible.
   */
  default <T> T to(final TypeLiteral<T> type, final String mtype) {
    return to(type, MediaType.valueOf(mtype));
  }

  /**
   * Convert a raw value to the given type. This method will temporary set {@link MediaType} before
   * parsing a value, useful if a form field from a HTTP POST was send as json (or any other data).
   *
   * @param type The type to convert to.
   * @param mtype A media type to hint a parser.
   * @param <T> Target type.
   * @return Get a value when possible.
   */
  <T> T to(TypeLiteral<T> type, MediaType mtype);

  /**
   * A map view of this mutant.
   *
   * If this mutant is the result of {@link Request#params()} the resulting map will have all the
   * available parameter names.
   *
   * If the mutant is the result of {@link Request#param(String)} the resulting map will have just
   * one entry, with the name as key.
   *
   * If the mutant is the result of {@link Request#body()} the resulting map will have just
   * one entry, with a key of <code>body</code>.
   *
   * @return A map view of this mutant.
   */
  Map<String, Mutant> toMap();

  /**
   * @return True if this mutant has a value (param, header, body, etc...).
   */
  boolean isSet();
}
