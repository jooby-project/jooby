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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.lang.reflect.Type;

public class WebSocketMessageImpl extends ByteArrayBody implements WebSocketMessage {

  private static class WebSocketMessageBody extends ForwardingContext implements DefaultContext {

    private final Body body;

    public WebSocketMessageBody(@NonNull Context context, Body body) {
      super(context);
      this.body = body;
    }

    @NonNull @Override public Body body() {
      return body;
    }

    @NonNull @Override public <T> T body(@NonNull Type type) {
      return body.to(type);
    }

    @NonNull @Override public <T> T body(@NonNull Class<T> type) {
      return body.to(type);
    }

    @NonNull @Override public <T> T decode(@NonNull Type type, @NonNull MediaType contentType) {
      return DefaultContext.super.decode(type, contentType);
    }
  }

  public WebSocketMessageImpl(Context ctx, byte[] bytes) {
    super(ctx, bytes);
  }

  @NonNull @Override public <T> T to(@NonNull Type type) {
    MediaType contentType = ctx.getRoute().getConsumes().get(0);
    return new WebSocketMessageBody(ctx, this).decode(type, contentType);
  }
}
