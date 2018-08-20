package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class DetachedContext extends Context.Forwarding {
  private final Route.After after;

  public DetachedContext(Context ctx, Route.After after) {
    super(ctx);
    this.after = after;
  }

  @Nonnull @Override public Context send(@Nonnull String data) {
    try {
      after.apply(ctx, data);
    } catch (Exception x) {
      ctx.sendError(x);
    }
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull byte[] data) {
    try {
      after.apply(ctx, data);
    } catch (Exception x) {
      ctx.sendError(x);
    }
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull String data, @Nonnull Charset charset) {
    try {
      after.apply(ctx, data);
    } catch (Exception x) {
      ctx.sendError(x);
    }
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuffer data) {
    try {
      after.apply(ctx, data);
    } catch (Exception x) {
      ctx.sendError(x);
    }
    return this;
  }
}
