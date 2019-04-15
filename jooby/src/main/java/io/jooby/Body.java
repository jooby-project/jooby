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

import io.jooby.internal.ByteArrayBody;
import io.jooby.internal.FileBody;
import io.jooby.internal.InputStreamBody;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;

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

  /* **********************************************************************************************
   * Factory methods:
   * **********************************************************************************************
   */

  /**
   * Empty body.
   *
   * @return Empty body.
   */
  static Body empty() {
    return ByteArrayBody.EMPTY;
  }

  /**
   * Creates a HTTP body from input stream.
   *
   * @param stream Input stream.
   * @param size Size in bytes or <code>-1</code>.
   * @return A new body.
   */
  static @Nonnull Body of(@Nonnull InputStream stream, long size) {
    return new InputStreamBody(stream, size);
  }

  /**
   * Creates a HTTP body from byte array.
   *
   * @param bytes byte array.
   * @return A new body.
   */
  static @Nonnull Body of(@Nonnull byte[] bytes) {
    return new ByteArrayBody(bytes);
  }

  /**
   * Creates a HTTP body from file.
   *
   * @param file File.
   * @return A new body.
   */
  static @Nonnull Body of(@Nonnull Path file) {
    return new FileBody(file);
  }
}
