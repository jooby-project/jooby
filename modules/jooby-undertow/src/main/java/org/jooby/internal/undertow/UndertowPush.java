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
package org.jooby.internal.undertow;

import java.util.Map;

import org.jooby.spi.NativePushPromise;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

public class UndertowPush implements NativePushPromise {

  private HttpServerExchange exchange;

  public UndertowPush(final HttpServerExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public void push(final String method, final String path, final Map<String, Object> headers) {
    HeaderMap h2headers = new HeaderMap();
    headers.forEach((n, v) -> h2headers.add(HttpString.tryFromString(n), v.toString()));
    exchange.getConnection().pushResource(path, HttpString.tryFromString(method), h2headers);
  }

}
