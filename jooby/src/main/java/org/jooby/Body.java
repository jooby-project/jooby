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

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.jooby.internal.SetHeaderImpl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;

/**
 * Utility class for generating HTTP responses from MVC routes (usually).
 *
 * <pre>
 * class MyRoute {
 *
 *   &#64;GET
 *   &#64;Path("/")
 *   public Body webMethod() {
 *     return Body.redirect("/somewhere")
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.1.0
 */
public class Body {

  public interface Parser {

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
     * {@link Body.Reader#text(Body.Reader.Text)} in order to apply correct charset and close
     * resources.
     * </p>
     *
     * <p>
     * For binary format a converter usually call to {@link Body.Reader#bytes(Body.Reader.Bytes)}
     * in order to close resources.
     * </p>
     *
     * @param type A type of message.
     * @param reader A read context.
     * @param <T> Target type.
     * @return A body message.
     * @throws Exception If read operation fail.
     */
    @Nonnull
    default <T> T parse(@Nonnull final Class<T> type, @Nonnull final Body.Reader reader)
        throws Exception {
      return parse(TypeLiteral.get(type), reader);
    }

    /**
     * Attempt to read a message from HTTP request body.
     * <p>
     * For text format (json, yaml, xml, etc.) a converter usually call to
     * {@link Body.Reader#text(Body.Reader.Text)} in order to apply correct charset and close
     * resources.
     * </p>
     *
     * <p>
     * For binary format a converter usually call to {@link Body.Reader#bytes(Body.Reader.Bytes)}
     * in order to close resources.
     * </p>
     *
     * @param type A type of message.
     * @param reader A read context.
     * @param <T> Target type.
     * @return A body message.
     * @throws Exception If read operation fail.
     */
    @Nonnull <T> T parse(@Nonnull TypeLiteral<T> type, @Nonnull Body.Reader reader) throws Exception;

  }

  public interface Formatter {

    /**
     * The <code>Accept</code> header is tested against this type in order to decided if this
     * formatter accept or not a type.
     *
     * @return Supported types.
     */
    List<MediaType> types();

    /**
     * Test if the given type can be write it to the HTTP response body.
     *
     * @param type The candidate type.
     * @return True if the converter can write into the HTTP response body.
     */
    boolean canFormat(@Nonnull Class<?> type);

    /**
     * Attempt to write a message into the HTTP response body.
     *
     * <p>
     * For text format (json, yaml, xml, etc.) a converter usually call to
     * {@link Body.Writer#text(Body.Writer.Text)} in order to set charset and close resources.
     * </p>
     *
     * <p>
     * For binary format a converter usually call to
     * {@link Body.Writer#bytes(Body.Writer.Bytes)} in order to close resources.
     * </p>
     *
     * @param body A body message.
     * @param writer A write context.
     * @throws Exception If write operation fail.
     */
    void format(@Nonnull Object body, @Nonnull Writer writer) throws Exception;

  }

  /**
   * Utility class to properly reading a HTTP request body or parameters. It provides methods for
   * reading text and bytes efficiently.
   * Clients shouldn't worry about closing the HTTP request body.
   *
   * @author edgar
   * @since 0.1.0
   */
  public interface Reader {

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
   * A write context. It provides methods for writing text and binary responses efficiently.
   * Access to current request locals is available too from: {@link #locals()}.
   *
   * @author edgar
   * @since 0.1.0
   */
  public interface Writer {

    /**
     * Write bytes to the HTTP Body.
     *
     * @author edgar
     * @since 0.1.0
     */
    interface Bytes {

      /**
       * Write bytes into the given {@link OutputStream}. The {@link OutputStream} will be close it
       * automatically after this call. Clients shouldn't worry about closing
       * the {@link OutputStream}.
       *
       * @param out The HTTP response body.
       * @throws Exception When the operation fails.
       */
      void write(OutputStream out) throws Exception;
    }

    /**
     * Write text to the HTTP Body and apply application/request charset.
     *
     * @author edgar
     * @since 0.1.0
     */
    interface Text {

      /**
       * Write text into the given {@link java.io.Writer}. The {@link java.io.Writer} will be
       * close it automatically after this call. Clients shouldn't worry about closing the
       * {@link Writer}. The writer is configured with the application/request charset.
       *
       * @param writer The HTTP response body.
       * @throws Exception When the operation fails.
       */
      void write(java.io.Writer writer) throws Exception;
    }

    /**
     * Send text to the client and takes care of charset.
     *
     * @author edgar
     * @since 0.5.0
     */
    interface Text2 {

      /**
       * Write text into the given {@link java.io.Writer}. The {@link java.io.Writer} will be
       * close it automatically after this call. Clients shouldn't worry about closing the
       * {@link Writer}. The writer is configured with the application/request charset.
       *
       * @param writer The HTTP response body.
       * @throws Exception When the operation fails.
       */
      void write(CharSequence text) throws Exception;
    }

    /**
     * @return A charset.
     */
    Charset charset();

    /**
     * Access to request locals. See {@link Request#attributes()} and {@link Locals}.
     *
     * @return Current request locals.
     */
    Map<String, Object> locals();

    /**
     * Write text into the HTTP response body using the {@link #charset()} and close the resources.
     * It applies the application/request charset.
     *
     * @param text A text strategy.
     * @throws Exception When the operation fails.
     */
    void text(@Nonnull Text text) throws Exception;

    /**
     * Write bytes into the HTTP response body and close the resources.
     *
     * @param bytes A bytes strategy.
     * @throws Exception When the operation fails.
     */
    void bytes(@Nonnull Bytes bytes) throws Exception;

  }

  /** Response headers. */
  private Map<String, String> headers = new LinkedHashMap<>();

  /** Response header setter. */
  private SetHeaderImpl setHeader = new SetHeaderImpl((name, value) -> headers.put(name, value));

  /** Response body. */
  private Object content;

  /** Response status. */
  private Status status;

  /** Response content-type. */
  private MediaType type;

  /**
   * Creates a new body with content.
   *
   * @param content A body content.
   */
  private Body(final Object content) {
    content(content);
  }

  /**
   * Creates a new body.
   */
  private Body() {
  }

  /**
   * Set the body content!
   *
   * @param content A content.
   * @return A new body.
   */
  public static @Nonnull Body body(final @Nonnull Object content) {
    return new Body(content);
  }

  /**
   * Set the response status.
   *
   * @param status A status!
   * @return A new body.
   */
  public static @Nonnull Body body(final @Nonnull Status status) {
    requireNonNull(status, "A HTTP status is required.");
    return new Body().status(status);
  }

  /**
   * Set the response status.
   *
   * @param status A status!
   * @return A new body.
   */
  public static @Nonnull Body body(final @Nonnull int status) {
    return body(Status.valueOf(status));
  }

  /**
   * @return A new body with {@link Status#OK}.
   */
  public static @Nonnull Body ok() {
    return body(Status.OK);
  }

  /**
   * @param content A body content!
   * @return A new body with {@link Status#OK} and given content.
   */
  public static @Nonnull Body ok(final @Nonnull Object content) {
    return ok().content(content);
  }

  /**
   * @return A new body with {@link Status#ACCEPTED}.
   */
  public static @Nonnull Body accepted() {
    return body(Status.ACCEPTED);
  }

  /**
   * @param content A body content!
   * @return A new body with {@link Status#ACCEPTED}.
   */
  public static @Nonnull Body accepted(final @Nonnull Object content) {
    return accepted().content(content);
  }

  /**
   * @return A new body with {@link Status#NO_CONTENT}.
   */
  public static @Nonnull Body noContent() {
    return body(Status.NO_CONTENT);
  }

  /**
   * Produces a redirect (302) status code and set the <code>Location</code> header too.
   *
   * @param location A location.
   * @return A new body.
   */
  public static @Nonnull Body redirect(final @Nonnull String location) {
    return redirect(Status.FOUND, location);
  }

  /**
   * Produces a redirect (307) status code and set the <code>Location</code> header too.
   *
   * @param location A location.
   * @return A new body.
   */
  public static @Nonnull Body tempRedirect(final @Nonnull String location) {
    return redirect(Status.TEMPORARY_REDIRECT, location);
  }

  /**
   * Produces a redirect (302) status code and set the <code>Location</code> header too.
   *
   * @param location A location.
   * @return A new body.
   */
  public static @Nonnull Body moved(final @Nonnull String location) {
    return redirect(Status.MOVED_PERMANENTLY, location);
  }

  /**
   * Produces a redirect (302) status code and set the <code>Location</code> header too.
   *
   * @param location A location.
   * @return A new body.
   */
  public static @Nonnull Body seeOther(final @Nonnull String location) {
    return redirect(Status.SEE_OTHER, location);
  }

  /**
   * Produces a redirect (302) status code and set the <code>Location</code> header too.
   *
   * @param status A HTTP redirect status.
   * @param location A location.
   * @return A new body.
   */
  private static Body redirect(final Status status, final String location) {
    requireNonNull(location, "A location is required.");
    return body(status).header("location", location);
  }

  /**
   * Set response status.
   *
   * @param status A new response status to use.
   * @return This body.
   */
  public @Nonnull Body status(final @Nonnull Status status) {
    this.status = requireNonNull(status, "A status is required.");
    return this;
  }

  /**
   * Set response status.
   *
   * @param status A new response status to use.
   * @return This body.
   */
  public @Nonnull Body status(final int status) {
    return status(Status.valueOf(status));
  }

  /**
   * Set the content type of this body.
   *
   * @param type A content type.
   * @return This body.
   */
  public @Nonnull Body type(final @Nonnull MediaType type) {
    this.type = requireNonNull(type, "A content type is required.");
    return this;
  }

  /**
   * Set the content type of this body.
   *
   * @param type A content type.
   * @return This body.
   */
  public @Nonnull Body type(final @Nonnull String type) {
    return type(MediaType.valueOf(type));
  }

  /**
   * Set body content.
   *
   * @param content A body!
   * @return This body.
   */
  public @Nonnull Body content(final @Nonnull Object content) {
    this.content = requireNonNull(content, "Content is required.");
    return this;
  }

  /**
   * @return Raw headers for body.
   */
  public @Nonnull Map<String, String> headers() {
    return ImmutableMap.copyOf(headers);
  }

  /**
   * @return Body status.
   */
  public @Nonnull Optional<Status> status() {
    return Optional.ofNullable(status);
  }

  /**
   * @return Body type.
   */
  public @Nonnull Optional<MediaType> type() {
    return Optional.ofNullable(type);
  }

  /**
   * @return Body content.
   */
  public @Nonnull Optional<Object> content() {
    return Optional.ofNullable(content);
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This body.
   */
  public @Nonnull Body header(final @Nonnull String name, final char value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This body.
   */
  public @Nonnull Body header(final @Nonnull String name, final byte value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This body.
   */
  public @Nonnull Body header(final @Nonnull String name, final short value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This body.
   */
  public @Nonnull Body header(final @Nonnull String name, final int value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This body.
   */
  public Body header(final String name, final long value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This body.
   */
  public @Nonnull Body header(final @Nonnull String name, final float value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This body.
   */
  public @Nonnull Body header(final @Nonnull String name, final double value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This body.
   */
  public @Nonnull Body header(final @Nonnull String name, final CharSequence value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This body.
   */
  public @Nonnull Body header(final @Nonnull String name, final @Nonnull Date value) {
    setHeader.header(name, value);
    return this;
  }

}
