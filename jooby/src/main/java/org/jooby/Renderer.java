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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

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
 *        .toInstance(renderer((value, ctx) {@literal ->} {
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
 *       ctx.text(value.toString());
 *     }
 *   });
 * </pre>
 *
 * For check for the <code>Accept</code> header:
 *
 * <pre>
 *   renderer((value, ctx) {@literal ->} {
 *     if (ctx.accepts("json")) {
 *       ctx.text(toJson(value));
 *     }
 *   });
 * </pre>
 *
 * API is simply and powerful! It is so powerful that you can override any of the existing built-in
 * renderers, because application specific renderers has precedence over built-in renderers.
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
     * @return Request attributes.
     */
    Map<String, Object> locals();

    /**
     * @param type The type to check for.
     * @return True if the given type matches the <code>Accept</code> header.
     */
    default boolean accepts(final String type) {
      return accepts(MediaType.valueOf(type));
    }

    /**
     * @param type The type to check for.
     * @return True if the given type matches the <code>Accept</code> header.
     */
    default boolean accepts(final MediaType type) {
      return accepts(ImmutableList.of(type));
    }

    /**
     * @param types Types to check for.
     * @return True if the given type matches the <code>Accept</code> header.
     */
    boolean accepts(List<MediaType> types);

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
     * Write bytes into the HTTP response body and close the resources.
     *
     * @param bytes A bytes to write.
     * @throws Exception When the operation fails.
     */
    void send(byte[] bytes) throws Exception;

    /**
     * Write bytes into the HTTP response body and close the resources.
     *
     * @param bytes A bytes to write.
     * @throws Exception When the operation fails.
     */
    void send(ByteBuffer buffer) throws Exception;

    /**
     * Write test into the HTTP response body.
     *
     * @param data A text to write.
     * @throws Exception When the operation fails.
     */
    void send(String text) throws Exception;

    /**
     * Write test into the HTTP response body.
     *
     * @param text A text to write.
     * @throws Exception When the operation fails.
     */
    void send(InputStream stream) throws Exception;

    /**
     * Write test into the HTTP response body.
     *
     * @param text A text to write.
     * @throws Exception When the operation fails.
     */
    void send(FileChannel file) throws Exception;

  }

  /**
   * Render the given value and write the response (if possible). If no response is written, the
   * next renderer in the chain will be invoked.
   *
   * @param object Object to render.
   * @param ctx Rendering context.
   * @throws Exception If rendering fails.
   */
  void render(Object object, Context ctx) throws Exception;
}
