/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.ByteArrayBody;
import io.jooby.internal.FileBody;
import io.jooby.internal.InputStreamBody;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * HTTP body value. Allows to access HTTP body as string, byte[], stream, etc..
 *
 * HTTP body can be read it only once per request. Attempt to read more than one resulted in
 * unexpected behaviour.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Body extends Value {

  /**
   * HTTP body as string.
   *
   * @param charset Charset.
   * @return Body as string.
   */
  default @Nonnull String value(@Nonnull Charset charset) {
    return new String(bytes(), charset);
  }

  /**
   * HTTP body as byte array.
   *
   * @return Body as byte array.
   */
  @Nonnull byte[] bytes();

  /**
   * True if body is on memory. False, indicates body is on file system. Body larger than
   * {@link ServerOptions#getMaxRequestSize()} will be dump to disk.
   *
   * @return True if body is on memory. False, indicates body is on file system.
   */
  boolean isInMemory();

  /**
   * Size in bytes. This is the same as <code>Content-Length</code> header.
   *
   * @return Size in bytes. This is the same as <code>Content-Length</code> header.
   */
  long getSize();

  /**
   * Body as readable channel.
   *
   * @return Body as readable channel.
   */
  @Nonnull ReadableByteChannel channel();

  /**
   * Body as input stream.
   *
   * @return Body as input stream.
   */
  @Nonnull InputStream stream();

  @Nonnull @Override default <T> List<T> toList(@Nonnull Class<T> type) {
    return to(Reified.list(type).getType());
  }

  @Override default @Nonnull <T> T to(@Nonnull Class<T> type) {
    return to((Type) type);
  }

  /**
   * Convert this body into the given type.
   *
   * @param type Type to use.
   * @param <T> Generic type.
   * @return Converted value.
   */
  @Nonnull <T> T to(@Nonnull Type type);

  /* **********************************************************************************************
   * Factory methods:
   * **********************************************************************************************
   */

  /**
   * Empty body.
   *
   * @param ctx Current context.
   * @return Empty body.
   */
  static @Nonnull Body empty(@Nonnull Context ctx) {
    return ByteArrayBody.empty(ctx);
  }

  /**
   * Creates a HTTP body from input stream.
   *
   * @param ctx Current context.
   * @param stream Input stream.
   * @param size Size in bytes or <code>-1</code>.
   * @return A new body.
   */
  static @Nonnull Body of(@Nonnull Context ctx, @Nonnull InputStream stream, long size) {
    return new InputStreamBody(ctx, stream, size);
  }

  /**
   * Creates a HTTP body from byte array.
   *
   * @param ctx Current context.
   * @param bytes byte array.
   * @return A new body.
   */
  static @Nonnull Body of(@Nonnull Context ctx, @Nonnull byte[] bytes) {
    return new ByteArrayBody(ctx, bytes);
  }

  /**
   * Creates a HTTP body from file.
   *
   * @param ctx Current context.
   * @param file File.
   * @return A new body.
   */
  static @Nonnull Body of(@Nonnull Context ctx, @Nonnull Path file) {
    return new FileBody(ctx, file);
  }
}
