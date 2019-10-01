/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.DefaultContext;
import io.jooby.ForwardingContext;
import io.jooby.MediaType;
import io.jooby.WebSocketMessage;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;

public class WebSocketMessageImpl extends ByteArrayBody implements WebSocketMessage {

  private static class WebSocketMessageBody extends ForwardingContext implements DefaultContext {

    private final Body body;

    public WebSocketMessageBody(@Nonnull Context context, Body body) {
      super(context);
      this.body = body;
    }

    @Nonnull @Override public Body body() {
      return body;
    }

    @Nonnull @Override public <T> T body(@Nonnull Type type) {
      return body.to(type);
    }

    @Nonnull @Override public <T> T body(@Nonnull Class<T> type) {
      return body.to(type);
    }

    @Nonnull @Override public <T> T decode(@Nonnull Type type, @Nonnull MediaType contentType) {
      return DefaultContext.super.decode(type, contentType);
    }
  }

  public WebSocketMessageImpl(Context ctx, byte[] bytes) {
    super(ctx, bytes);
  }

  @Nonnull @Override public <T> T to(@Nonnull Type type) {
    MediaType contentType = ctx.getRoute().getConsumes().get(0);
    return new WebSocketMessageBody(ctx, this).decode(type, contentType);
  }
}
