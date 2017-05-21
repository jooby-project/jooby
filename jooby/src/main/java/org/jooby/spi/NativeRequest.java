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
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.jooby.Cookie;

/**
 * Minimal/basic implementation of HTTP request. A server implementor must provide an implementation
 * of {@link NativeRequest}.
 *
 * @author edgar
 * @since 0.5.0
 */
public interface NativeRequest {
  /**
   * @return The name of the HTTP method with which this request was made, for example, GET, POST,
   *         or PUT.
   */
  String method();

  /**
   * @return The part of this request's URL from the protocol name up to the query string in the
   *         first line of the HTTP request.
   */
  String path();

  /**
   * Returns the query string that is contained in the request URL after the path.
   *
   * @return Query string or <code>empty</code>
   */
  Optional<String> queryString();

  /**
   * @return List with all the parameter names from query string plus any other form/multipart param
   *         names (excluding file uploads). This method should NOT returns null, absence of params
   *         is represented by an empty list.
   * @throws Exception If param extraction fails.
   */
  List<String> paramNames() throws Exception;

  /**
   * Get all the params for the provided name or a empty list.
   *
   * @param name Parameter name.
   * @return Get all the params for the provided name or a empty list.
   * @throws Exception If param parsing fails.
   */
  List<String> params(String name) throws Exception;

  /**
   * @return Map containing all request attributes
   */
  @SuppressWarnings("unchecked")
  default Map<String, Object> attributes() {
    return Collections.EMPTY_MAP;
  }

  /**
   * Get all the headers for the provided name or a empty list.
   *
   * @param name Header name.
   * @return Get all the headers for the provided name or a empty list.
   */
  List<String> headers(String name);

  /**
   * Get the first header for the provided name or a empty list.
   *
   * @param name Header name.
   * @return The first header for the provided name or a empty list.
   */
  Optional<String> header(final String name);

  /**
   * @return All the header names or an empty list.
   */
  List<String> headerNames();

  /**
   * @return All the cookies or an empty list.
   */
  List<Cookie> cookies();

  /**
   * Get all the files for the provided name or an empty list.
   *
   * @param name File name.
   * @return All the files or an empty list.
   * @throws IOException If file parsing fails.
   */
  List<NativeUpload> files(String name) throws IOException;

  /**
   * Input stream that represent the body.
   *
   * @return Body as an input stream.
   * @throws IOException If body read fails.
   */
  InputStream in() throws IOException;

  /**
   * @return The IP address of the client or last proxy that sent the request.
   */
  String ip();

  /**
   * @return The name and version of the protocol the request uses in the form
   *         <i>protocol/majorVersion.minorVersion</i>, for example, HTTP/1.1
   */
  String protocol();

  /**
   * @return True if this request was made using a secure channel, such as HTTPS.
   */
  boolean secure();

  /**
   * Upgrade the request to something else...like a web socket.
   *
   * @param type Upgrade type.
   * @param <T> Upgrade type.
   * @return A instance of the upgrade.
   * @throws Exception If the upgrade fails or it is un-supported.
   * @see NativeWebSocket
   */
  <T> T upgrade(Class<T> type) throws Exception;

  /**
   * Put the request in async mode.
   *
   * @param executor Executor to use.
   * @param runnable Task to run.
   */
  void startAsync(Executor executor, Runnable runnable);

  /**
   * Send push promise to the client.
   *
   * @param method HTTP method.
   * @param path HTTP path.
   * @param headers HTTP headers.
   * @throws Exception If something goes wrong.
   */
  default void push(final String method, final String path, final Map<String, Object> headers)
      throws Exception {
    upgrade(NativePushPromise.class).push(method, path, headers);
  }

  /**
   * @return Request path without decoding.
   */
  String rawPath();
}
