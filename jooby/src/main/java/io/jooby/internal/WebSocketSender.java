/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.DefaultContext;
import io.jooby.ForwardingContext;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.jooby.WebSocket;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;

public class WebSocketSender extends ForwardingContext implements DefaultContext {

  private final WebSocket ws;

  public WebSocketSender(@NonNull Context context, @NonNull WebSocket ws) {
    super(context);
    this.ws = ws;
  }

  @NonNull @Override public Context send(@NonNull String data, @NonNull Charset charset) {
    ws.send(data);
    return this;
  }

  @NonNull @Override public Context send(@NonNull byte[] data) {
    ws.send(data);
    return this;
  }

  @NonNull @Override public Context render(@NonNull Object value) {
    DefaultContext.super.render(value);
    return this;
  }

  @Override public Context setResetHeadersOnError(boolean value) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setDefaultResponseType(@NonNull MediaType contentType) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseCode(int statusCode) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseCode(@NonNull StatusCode statusCode) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseCookie(@NonNull Cookie cookie) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseHeader(@NonNull String name, @NonNull String value) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseHeader(@NonNull String name, @NonNull Date value) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseHeader(@NonNull String name, @NonNull Object value) {
    // NOOP
    return this;
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull Instant value) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseLength(long length) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseType(@NonNull String contentType) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseType(@NonNull MediaType contentType,
      @Nullable Charset charset) {
    // NOOP
    return this;
  }

  @NonNull @Override public Context setResponseType(@NonNull MediaType contentType) {
    // NOOP
    return this;
  }
}
