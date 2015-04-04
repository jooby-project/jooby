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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import org.jooby.Cookie;

/**
 * Minimal/basic implementation of HTTP request. A server implementor must provide an implementation
 * of {@link NativeResponse}.
 *
 * @author edgar
 * @since 0.5.0
 */
public interface NativeResponse {

  /**
   * Set a response cookie.
   *
   * @param cookie A cookie to add.
   */
  void cookie(Cookie cookie);

  /**
   * Clear a cookie and force a client to expire and delete it.
   *
   * @param name Cookie's name.
   */
  void clearCookie(String name);

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
   * @param value Header's value.
   */
  void header(String name, String value);

  /**
   * Get an output stream and prepare to send a response. You need to make sure status and headers
   * are set before calling this method. Attempt to set status and headers after calling output
   * stream if undefined and might result in error or just ignored.
   *
   * @param bufferSize The preferred buffer size for sending a response. Default buffer size is:
   *        <code>16k</code>. Default buffer size by setting the:
   *        <code>server.http.ResponseBufferSize</code> property in your <code>*.conf</code>
   *        property file.
   *        If the <code>Content-Length</code> header was set and it is less than buffer size, the
   *        the <code>Content-Length</code> will be used it as buffer size.
   * @return An output stream.
   * @throws IOException If output stream can be acquire it.
   */
  OutputStream out(int bufferSize) throws IOException;

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
