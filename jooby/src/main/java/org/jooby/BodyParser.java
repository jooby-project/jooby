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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.inject.TypeLiteral;

/**
 * Read a series of bytes and build a Java Object. A parser is responsible for converting bytes
 * in a JSON, XML, binary format to a Java Object.
 *
 * @author edgar
 * @since 0.1.0
 * @since 0.5.0
 */
public interface BodyParser {

  /**
   * Utility class for reading a HTTP request body. It provides methods for reading text and bytes
   * safely.
   * Clients shouldn't worry about closing the HTTP request body.
   *
   * @author edgar
   * @since 0.1.0
   */
  public interface Context {

    /**
     * Read bytes from HTTP Body.
     *
     * @author edgar
     * @since 0.1.0
     */
    interface Bytes {

      /**
       * Read bytes from the given {@link InputStream}. The {@link InputStream} will be close it
       * automatically after this call. Clients shouldn't worry about closing the
       * {@link InputStream}.
       *
       * @param in The HTTP request body.
       * @return A body result (usually) converted to something else.
       * @throws Exception When the operation fails.
       */
      Object read(java.io.InputStream in) throws Exception;
    }

    /**
     * Read text from HTTP body.
     *
     * @author edgar
     * @since 0.1.0
     */
    interface Text {

      /**
       * Read text from the given {@link java.io.Reader}. The {@link java.io.Reader} will be close
       * it automatically after this call. Clients shouldn't worry about closing the
       * {@link java.io.Reader}.
       *
       * @param reader The HTTP request body.
       * @return A body result (usually) converted to something else.
       * @throws Exception When the operation fails.
       */
      Object read(java.io.Reader reader) throws Exception;

    }

    /**
     * Convert a HTTP request body to something else. The body must be read it using the request
     * {@link Charset}.
     *
     * @param text A text reading strategy.
     * @param <T> Target type.
     * @return A HTTP body converted to something else.
     * @throws Exception When the operation fails.
     */
    @Nonnull
    <T> T text(@Nonnull Text text) throws Exception;

    /**
     * Convert a HTTP request body to something else.
     *
     * @param bytes A bytes reading strategy.
     * @param <T> Target type.
     * @return A HTTP body converted to something else.
     * @throws Exception When the operation fails.
     */
    @Nonnull
    <T> T bytes(@Nonnull Bytes bytes) throws Exception;

  }

  /**
   * The <code>Content-Type</code> header is tested against this type in order to decided if this
   * parser accept or not a type.
   *
   * @return Supported types.
   */
  List<MediaType> types();

  /**
   * Test if the HTTP request body or parameter can be converted to the given type.
   *
   * @param type The candidate Type.
   * @return True if the converter can read the HTTP request body.
   */
  default boolean canParse(@Nonnull final Class<?> type) {
    return canParse(TypeLiteral.get(type));
  }

  /**
   * Test if the HTTP request body or parameter can be converted to the given type.
   *
   * @param type The candidate Type.
   * @return True if the converter can read the HTTP request body.
   */
  boolean canParse(@Nonnull TypeLiteral<?> type);

  /**
   * Attempt to read a message from HTTP request body.
   * <p>
   * For text format (json, yaml, xml, etc.) a converter usually call to
   * {@link Context#text(Context.Text)} in order to apply correct charset and close
   * resources.
   * </p>
   *
   * <p>
   * For binary format a converter usually call to {@link Context#bytes(Context.Bytes)} in
   * order to close resources.
   * </p>
   *
   * @param type A type of message.
   * @param ctx A read context.
   * @param <T> Target type.
   * @return A body message.
   * @throws Exception If read operation fail.
   */
  @Nonnull
  default <T> T parse(@Nonnull final Class<T> type, @Nonnull final Context ctx)
      throws Exception {
    return parse(TypeLiteral.get(type), ctx);
  }

  /**
   * Attempt to read a message from HTTP request body.
   * <p>
   * For text format (json, yaml, xml, etc.) a converter usually call to
   * {@link Context#text(Context.Text)} in order to apply correct charset and close
   * resources.
   * </p>
   *
   * <p>
   * For binary format a converter usually call to {@link Context#bytes(Context.Bytes)} in
   * order to close resources.
   * </p>
   *
   * @param type A type of message.
   * @param ctx A read context.
   * @param <T> Target type.
   * @return A body message.
   * @throws Exception If read operation fail.
   */
  @Nonnull
  <T> T parse(@Nonnull TypeLiteral<T> type, @Nonnull BodyParser.Context ctx) throws Exception;

}
