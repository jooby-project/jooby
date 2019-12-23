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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;

public class WebSocketSender extends ForwardingContext implements DefaultContext {

  private final WebSocket ws;

  public WebSocketSender(@Nonnull Context context, @Nonnull WebSocket ws) {
    super(context);
    this.ws = ws;
  }

  @Nonnull @Override public Context send(@Nonnull String data, @Nonnull Charset charset) {
    ws.send(data);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull byte[] data) {
    ws.send(data);
    return this;
  }

  @Nonnull @Override public Context render(@Nonnull Object value) {
    DefaultContext.super.render(value);
    return this;
  }

  @Override public Context setResetHeadersOnError(boolean value) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setDefaultResponseType(@Nonnull MediaType contentType) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseCode(int statusCode) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseCode(@Nonnull StatusCode statusCode) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseCookie(@Nonnull Cookie cookie) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull String value) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull Date value) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull Object value) {
    // NOOP
    return this;
  }

  @Nonnull @Override
  public Context setResponseHeader(@Nonnull String name, @Nonnull Instant value) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseLength(long length) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseType(@Nonnull String contentType) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseType(@Nonnull MediaType contentType,
      @Nullable Charset charset) {
    // NOOP
    return this;
  }

  @Nonnull @Override public Context setResponseType(@Nonnull MediaType contentType) {
    // NOOP
    return this;
  }
}
