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

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Format a Java object into bytes and/or text. A formatter is responsible for converting a Java
 * Object to HTML, JSON, XML, etc..
 *
 * @author edgar
 * @since 0.1.0
 * @since 0.5.0
 */
public interface BodyFormatter {

  /**
   * A write context. It provides methods for writing text and binary responses safely.
   * Access to current request locals is available too from: {@link #locals()}.
   *
   * @author edgar
   * @since 0.1.0
   */
  public interface Context {

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
       * {@link Context}. The writer is configured with the application/request charset.
       *
       * @param writer The HTTP response body.
       * @throws Exception When the operation fails.
       */
      void write(java.io.Writer writer) throws Exception;
    }

    /**
     * @return A charset.
     */
    Charset charset();

    /**
     * Access to request locals. See {@link Request#attributes()}.
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
    void text(Text text) throws Exception;

    /**
     * Write bytes into the HTTP response body and close the resources.
     *
     * @param bytes A bytes strategy.
     * @throws Exception When the operation fails.
     */
    void bytes(Bytes bytes) throws Exception;

  }

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
  boolean canFormat(Class<?> type);

  /**
   * Attempt to write a message into the HTTP response body.
   *
   * <p>
   * For text format (json, yaml, xml, etc.) a converter usually call to
   * {@link BodyFormatter.Context#text(BodyFormatter.Context.Text)} in order
   * to set charset and close resources.
   * </p>
   *
   * <p>
   * For binary format a converter usually call to
   * {@link BodyFormatter.Context#bytes(BodyFormatter.Context.Bytes)} in order
   * to close resources.
   * </p>
   *
   * @param body A body message.
   * @param ctx A write context.
   * @throws Exception If write operation fail.
   */
  void format(Object body, BodyFormatter.Context ctx) throws Exception;

}
