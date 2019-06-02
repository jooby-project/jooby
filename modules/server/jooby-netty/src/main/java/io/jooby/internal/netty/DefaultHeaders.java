/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
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

  private static final AsciiString server = AsciiString.cached("netty");

  private volatile AsciiString date = new AsciiString(FORMATTER.format(Instant.now()));

  @Override public void run() {
    date = new AsciiString(FORMATTER.format(Instant.now()));
  }

  @Override public void accept(HttpHeaders headers) {
    headers.set(HttpHeaderNames.DATE, date);
    headers.set(HttpHeaderNames.SERVER, server);
    headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
  }
}
