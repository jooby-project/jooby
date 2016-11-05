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
package org.jooby.spi;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Optional;

/**
 * Minimal/basic implementation of HTTP request. A server implementor must provide an implementation
 * of {@link NativeResponse}.
 *
 * @author edgar
 * @since 0.5.0
 */
public interface NativeResponse {

  /**
   * Get a response header (previously set).
   *
   * @param name Header's name.
   * @return Header.
   */
  Optional<String> header(final String name);

  /**
   * Get all the response headers for the provided name.
   *
   * @param name A header's name
   * @return All the response headers.
   */
  List<String> headers(String name);

  /**
   * Set a response header.
   *
   * @param name Header's name.
   * @param values Header's values.
   */
  void header(String name, Iterable<String> values);

  /**
   * Set a response header.
   *
   * @param name Header's name.
   * @param value Header's value.
   */
  void header(String name, String value);

  void send(byte[] bytes) throws Exception;

  void send(ByteBuffer buffer) throws Exception;

  void send(InputStream stream) throws Exception;

  void send(FileChannel channel) throws Exception;

  void send(FileChannel channel, long possition, long count) throws Exception;

  /**
   * @return HTTP response status.
   */
  int statusCode();

  /**
   * Set the HTTP response status.
   *
   * @param code A HTTP response status.
   */
  void statusCode(int code);

  /**
   * @return True if response was committed to the client.
   */
  boolean committed();

  /**
   * End a response and clean up any resources used it.
   */
  void end();

  /**
   * Reset the HTTP status, headers and response buffer is need it.
   */
  void reset();

}
