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
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

import com.google.common.base.CaseFormat;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Write a value into the HTTP response and apply a format, if need it.
 *
 * Renderers are executed in the order they were registered. The first renderer that write a
 * response wins!
 *
 * There are two ways of registering a rendering:
 *
 * <pre>
 * {
 *   renderer((value, ctx) {@literal ->} {
 *     ...
 *   });
 * }
 * </pre>
 *
 * Or from inside a module:
 *
 * <pre>
 * {
 *   use((env, conf, binder) {@literal ->} {
 *     Multibinder.newSetBinder(binder, Renderer.class)
 *        .addBinding()
 *        .toInstance((value, ctx) {@literal ->} {
 *          ...
 *        }));
 *   });
 * }
 * </pre>
 *
 * Inside a {@link Renderer} you can do whatever you want. For example you can check for a specific
 * type:
 *
 * <pre>
 *   renderer((value, ctx) {@literal ->} {
 *     if (value instanceof MyObject) {
 *       ctx.send(value.toString());
 *     }
 *   });
 * </pre>
 *
 * Or check for the <code>Accept</code> header:
 *
 * <pre>
 *   renderer((value, ctx) {@literal ->} {
 *     if (ctx.accepts("json")) {
 *       ctx.send(toJson(value));
 *     }
 *   });
 * </pre>
 *
 * API is simple and powerful!
 *
 * @author edgar
 * @since 0.6.0
 */
public interface Renderer {

  /**
   * Contains a few utility methods for doing the actual rendering and writing.
   *
   * @author edgar
   * @since 0.6.0
   */
  interface Context {

    /**
     * @return Request local attributes.
     */
    Map<String, Object> locals();

    /**
     * True if the given type matches the <code>Accept</code> header.
     *
     * @param type The type to check for.
     * @return True if the given type matches the <code>Accept</code> header.
     */
    default boolean accepts(final String type) {
      return accepts(MediaType.valueOf(type));
    }

    /**
     * True if the given type matches the <code>Accept</code> header.
     *
     * @param type The type to check for.
     * @return True if the given type matches the <code>Accept</code> header.
     */
    boolean accepts(final MediaType type);

    /**
     * Set the <code>Content-Type</code> header IF and ONLY IF, no <code>Content-Type</code> was set
     * yet.
     *
     * @param type A suggested type to use if one is missing.
     * @return This context.
     */
    default Context type(final String type) {
      return type(MediaType.valueOf(type));
    }

    /**
     * Set the <code>Content-Type</code> header IF and ONLY IF, no <code>Content-Type</code> was set
     * yet.
     *
     * @param type A suggested type to use if one is missing.
     * @return This context.
     */
    Context type(MediaType type);

    /**
     * Set the <code>Content-Length</code> header IF and ONLY IF, no <code>Content-Length</code> was
     * set yet.
     *
     * @param length A suggested length to use if one is missing.
     * @return This context.
     */
    Context length(long length);

    /**
     * @return Charset to use while writing text responses.
     */
    Charset charset();

    /**
     * Write bytes into the HTTP response body.
     *
     * It will set a <code>Content-Length</code> if none was set
     * It will set a <code>Content-Type</code> to {@link MediaType#octetstream} if none was set.
     *
     * @param bytes A bytes to write.
     * @throws Exception When the operation fails.
     */
    void send(byte[] bytes) throws Exception;

    /**
     * Write byte buffer into the HTTP response body.
     *
     * It will set a <code>Content-Length</code> if none was set.
     * It will set a <code>Content-Type</code> to {@link MediaType#octetstream} if none was set.
     *
     * @param buffer A buffer to write.
     * @throws Exception When the operation fails.
     */
    void send(ByteBuffer buffer) throws Exception;

    /**
     * Write text into the HTTP response body.
     *
     * It will set a <code>Content-Length</code> if none was set.
     * It will set a <code>Content-Type</code> to {@link MediaType#html} if none was set.
     *
     * @param text A text to write.
     * @throws Exception When the operation fails.
     */
    void send(String text) throws Exception;

    /**
     * Write bytes into the HTTP response body.
     *
     * It will set a <code>Content-Length</code> if the response size is less than the
     * <code>server.ResponseBufferSize</code> (default is: 16k). If the response is larger than the
     * buffer size, it will set a <code>Transfer-Encoding: chunked</code> header.
     *
     * It will set a <code>Content-Type</code> to {@link MediaType#octetstream} if none was set.
     *
     * This method will check if the given input stream has a {@link FileChannel} and redirect to
     * file
     *
     * @param stream Bytes to write.
     * @throws Exception When the operation fails.
     */
    void send(InputStream stream) throws Exception;

    /**
     * Write text into the HTTP response body.
     *
     * It will set a <code>Content-Length</code> if none was set.
     * It will set a <code>Content-Type</code> to {@link MediaType#html} if none was set.
     *
     * @param buffer A text to write.
     * @throws Exception When the operation fails.
     */
    void send(CharBuffer buffer) throws Exception;

    /**
     * Write text into the HTTP response body.
     *
     * It will set a <code>Content-Length</code> if the response size is less than the
     * <code>server.ResponseBufferSize</code> (default is: 16k). If the response is larger than the
     * buffer size, it will set a <code>Transfer-Encoding: chunked</code> header.
     *
     * It will set a <code>Content-Type</code> to {@link MediaType#html} if none was set.
     *
     * @param reader Text to write.
     * @throws Exception When the operation fails.
     */
    void send(Reader reader) throws Exception;

    /**
     * Write file into the HTTP response body, using OS zero-copy transfer (if possible).
     *
     * It will set a <code>Content-Length</code> if none was set.
     * It will set a <code>Content-Type</code> to {@link MediaType#html} if none was set.
     *
     * @param file A text to write.
     * @throws Exception When the operation fails.
     */
    void send(FileChannel file) throws Exception;

  }

  /** Renderer key. */
  Key<Set<Renderer>> KEY = Key.get(new TypeLiteral<Set<Renderer>>() {
  });

  /**
   * @return Renderer's name.
   */
  default String name() {
    String name = getClass().getSimpleName()
        .replace("renderer", "")
        .replace("render", "");
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name);
  }

  /**
   * Render the given value and write the response (if possible). If no response is written, the
   * next renderer in the chain will be invoked.
   *
   * @param value Object to render.
   * @param ctx Rendering context.
   * @throws Exception If rendering fails.
   */
  void render(Object value, Context ctx) throws Exception;

  /**
   * Renderer factory method.
   *
   * @param name Renderer's name.
   * @param renderer Renderer's function.
   * @return A new renderer.
   */
  static Renderer of(final String name, final Renderer renderer) {
    return new Renderer() {
      @Override
      public void render(final Object value, final Context ctx) throws Exception {
        renderer.render(value, ctx);
      }

      @Override
      public String name() {
        return name;
      }
    };
  }
}
