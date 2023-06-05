/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.exception.MissingValueException;
import io.jooby.internal.ByteArrayBody;
import io.jooby.internal.FileBody;
import io.jooby.internal.InputStreamBody;

/**
 * HTTP body value. Allows to access HTTP body as string, byte[], stream, etc..
 *
 * <p>HTTP body can be read it only once per request. Attempt to read more than one resulted in
 * unexpected behaviour.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Body extends ValueNode {

  /**
   * HTTP body as string.
   *
   * @param charset Charset.
   * @return Body as string.
   */
  default @NonNull String value(@NonNull Charset charset) {
    byte[] bytes = bytes();
    if (bytes.length == 0) {
      throw new MissingValueException("body");
    }
    return new String(bytes, charset);
  }

  /**
   * HTTP body as byte array.
   *
   * @return Body as byte array.
   */
  @NonNull byte[] bytes();

  /**
   * True if body is on memory. False, indicates body is on file system. Body larger than {@link
   * ServerOptions#getMaxRequestSize()} will be dump to disk.
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
  @NonNull ReadableByteChannel channel();

  /**
   * Body as input stream.
   *
   * @return Body as input stream.
   */
  @NonNull InputStream stream();

  @NonNull @Override
  default <T> List<T> toList(@NonNull Class<T> type) {
    return to(Reified.list(type).getType());
  }

  default @NonNull @Override List<String> toList() {
    return Collections.singletonList(value());
  }

  default @NonNull @Override Set<String> toSet() {
    return Collections.singleton(value());
  }

  @Override
  default @NonNull <T> T to(@NonNull Class<T> type) {
    return to((Type) type);
  }

  default @Nullable @Override <T> T toNullable(@NonNull Class<T> type) {
    return toNullable((Type) type);
  }

  /**
   * Convert this body into the given type.
   *
   * @param type Type to use.
   * @param <T> Generic type.
   * @return Converted value.
   */
  @NonNull <T> T to(@NonNull Type type);

  /**
   * Convert this body into the given type.
   *
   * @param type Type to use.
   * @param <T> Generic type.
   * @return Converted value or <code>null</code>.
   */
  @Nullable <T> T toNullable(@NonNull Type type);

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
  static @NonNull Body empty(@NonNull Context ctx) {
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
  static @NonNull Body of(@NonNull Context ctx, @NonNull InputStream stream, long size) {
    return new InputStreamBody(ctx, stream, size);
  }

  /**
   * Creates a HTTP body from byte array.
   *
   * @param ctx Current context.
   * @param bytes byte array.
   * @return A new body.
   */
  static @NonNull Body of(@NonNull Context ctx, @NonNull byte[] bytes) {
    return new ByteArrayBody(ctx, bytes);
  }

  /**
   * Creates a HTTP body from file.
   *
   * @param ctx Current context.
   * @param file File.
   * @return A new body.
   */
  static @NonNull Body of(@NonNull Context ctx, @NonNull Path file) {
    return new FileBody(ctx, file);
  }
}
