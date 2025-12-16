/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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

    public WebSocketMessageBody(@NonNull Context context, Body body) {
      super(context);
      this.body = body;
    }

    @NonNull @Override
    public Body body() {
      return body;
    }

    @NonNull @Override
    public <T> T body(@NonNull Type type) {
      return body.to(type);
    }

    @NonNull @Override
    public <T> T body(@NonNull Class<T> type) {
      return body.to(type);
    }

    @NonNull @Override
    public <T> T decode(@NonNull Type type, @NonNull MediaType contentType) {
      return DefaultContext.super.decode(type, contentType);
    }
  }

  public WebSocketMessageImpl(Context ctx, byte[] bytes) {
    super(ctx, bytes);
  }

  @NonNull @Override
  public <T> T to(@NonNull Type type) {
    MediaType contentType = ctx.getRoute().getConsumes().get(0);
    return new WebSocketMessageBody(ctx, this).decode(type, contentType);
  }

  @Override
  public Value get(@NonNull String name) {
    return new MissingValue(ctx.getValueFactory(), name);
  }

  @Override
  public Value getOrDefault(@NonNull String name, @NonNull String defaultValue) {
    return Value.value(ctx.getValueFactory(), name, defaultValue);
  }

  @Nullable @Override
  public <T> T toNullable(@NonNull Type type) {
    return this.to(type);
  }

  @Override
  @NonNull public byte[] bytes() {
    return super.bytes();
  }

  @Override
  public @NonNull ByteBuffer byteBuffer() {
    return ByteBuffer.wrap(bytes());
  }
}
