/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;

import org.eclipse.jetty.server.Response;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Sender;

public class JettySender implements Sender {
  private final JettyContext ctx;
  private final Response response;

  public JettySender(JettyContext ctx, Response response) {
    this.ctx = ctx;
    this.response = response;
  }

  @Override
  public Sender write(@NonNull byte[] data, @NonNull Callback callback) {
    response.write(false, ByteBuffer.wrap(data), toJettyCallback(ctx, callback));
    return this;
  }

  private static org.eclipse.jetty.util.Callback toJettyCallback(
      JettyContext ctx, Callback callback) {
    return new org.eclipse.jetty.util.Callback() {

      @Override
      public void succeeded() {
        callback.onComplete(ctx, null);
      }

      @Override
      public void failed(Throwable x) {
        callback.onComplete(ctx, x);
      }
    };
  }

  @Override
  public void close() {
    response.write(false, null, ctx);
  }
}
