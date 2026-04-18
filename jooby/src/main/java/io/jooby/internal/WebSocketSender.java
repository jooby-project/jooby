/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.DefaultContext;
import io.jooby.ForwardingContext;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.jooby.WebSocket;
import io.jooby.output.Output;

public class WebSocketSender extends ForwardingContext implements DefaultContext {

  private final WebSocket ws;
  private final boolean binary;
  private final WebSocket.WriteCallback callback;

  public WebSocketSender(
      Context context, WebSocket ws, boolean binary, WebSocket.WriteCallback callback) {
    super(context);
    this.ws = ws;
    this.binary = binary;
    this.callback = callback;
  }

  @Override
  public Context send(String data, Charset charset) {
    if (binary) {
      ws.sendBinary(data.getBytes(charset), callback);
    } else {
      ws.send(data.getBytes(charset), callback);
    }
    return this;
  }

  @Override
  public Context send(byte[] data) {
    if (binary) {
      ws.sendBinary(data, callback);
    } else {
      ws.send(data, callback);
    }
    return this;
  }

  @Override
  public Context send(ByteBuffer data) {
    if (binary) {
      ws.sendBinary(data, callback);
    } else {
      ws.send(data, callback);
    }
    return this;
  }

  @Override
  public Context send(Output output) {
    if (binary) {
      ws.sendBinary(output, callback);
    } else {
      ws.send(output, callback);
    }
    return this;
  }

  @Override
  public Context render(Object value) {
    DefaultContext.super.render(value);
    return this;
  }

  @Override
  public Context setResetHeadersOnError(boolean value) {
    // NOOP
    return this;
  }

  @Override
  public Context setDefaultResponseType(MediaType contentType) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseCode(int statusCode) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseCode(StatusCode statusCode) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseCookie(Cookie cookie) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseHeader(String name, String value) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseHeader(String name, Date value) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseHeader(String name, Object value) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseHeader(String name, Instant value) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseLength(long length) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseType(String contentType) {
    // NOOP
    return this;
  }

  @Override
  public Context setResponseType(MediaType contentType) {
    // NOOP
    return this;
  }
}
