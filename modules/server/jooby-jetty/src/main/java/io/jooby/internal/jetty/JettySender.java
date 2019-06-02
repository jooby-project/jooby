/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import io.jooby.Sender;
import org.eclipse.jetty.server.HttpOutput;

import javax.annotation.Nonnull;
import java.io.IOException;

public class JettySender implements Sender {
  private final JettyContext ctx;
  private final HttpOutput sender;

  public JettySender(JettyContext ctx, HttpOutput sender) {
    this.ctx = ctx;
    this.sender = sender;
  }

  @Override public Sender write(@Nonnull byte[] data, @Nonnull Callback callback) {
    try {
      sender.write(data);
      sender.flush();
      callback.onComplete(ctx, null);
    } catch (IOException e) {
      callback.onComplete(ctx, e);
    }
    return this;
  }

  @Override public void close() {
    ctx.complete(null);
  }

}
