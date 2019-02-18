/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.jooby.Context;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class DefaultHeaders implements Consumer<HttpHeaders>, Runnable {

  private static final DateTimeFormatter FORMATTER = Context.RFC1123;

  private volatile AsciiString date = new AsciiString(FORMATTER.format(Instant.now()));

  private final AsciiString server = new AsciiString("netty");

  @Override public void run() {
    date = new AsciiString(FORMATTER.format(Instant.now()));
  }

  @Override public void accept(HttpHeaders headers) {
    headers.set(HttpHeaderNames.DATE, date);
    headers.set(HttpHeaderNames.SERVER, server);
    headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
  }
}
