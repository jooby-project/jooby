/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.jspecify.annotations.Nullable;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.DefaultContext;
import io.jooby.ForwardingContext;
import io.jooby.MediaType;
import io.jooby.WebSocketMessage;
import io.jooby.value.Value;

public class WebSocketMessageImpl extends ByteArrayBody implements WebSocketMessage {

  private static class WebSocketMessageBody extends ForwardingContext implements DefaultContext {

    private final Body body;

    public WebSocketMessageBody(Context context, Body body) {
      super(context);
      this.body = body;
    }

    @Override
    public Body body() {
      return body;
    }

    @Override
    public <T> T body(Type type) {
      return body.to(type);
    }

    @Override
    public <T> T body(Class<T> type) {
      return body.to(type);
    }

    @Override
    public <T> T decode(Type type, MediaType contentType) {
      return DefaultContext.super.decode(type, contentType);
    }
  }

  public WebSocketMessageImpl(Context ctx, byte[] bytes) {
    super(ctx, bytes);
  }

  @Override
  public <T> T to(Type type) {
    MediaType contentType = ctx.getRoute().getConsumes().get(0);
    return new WebSocketMessageBody(ctx, this).decode(type, contentType);
  }

  @Override
  public Value get(String name) {
    return new MissingValue(ctx.getValueFactory(), name);
  }

  @Override
  public Value getOrDefault(String name, String defaultValue) {
    return Value.value(ctx.getValueFactory(), name, defaultValue);
  }

  @Nullable @Override
  public <T> T toNullable(Type type) {
    return this.to(type);
  }

  @Override
  public byte[] bytes() {
    return super.bytes();
  }

  @Override
  public ByteBuffer byteBuffer() {
    return ByteBuffer.wrap(bytes());
  }
}
