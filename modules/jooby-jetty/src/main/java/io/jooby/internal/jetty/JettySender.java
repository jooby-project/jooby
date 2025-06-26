/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static io.jooby.internal.jetty.JettyCallbacks.fromDataBuffer;
import static io.jooby.internal.jetty.JettyCallbacks.fromOutput;

import java.nio.ByteBuffer;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Sender;
import io.jooby.buffer.DataBuffer;
import io.jooby.output.Output;

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

  @NonNull @Override
  public Sender write(@NonNull DataBuffer data, @NonNull Callback callback) {
    fromDataBuffer(response, toJettyCallback(ctx, callback), data).send(false);
    return this;
  }

  @NonNull @Override
  public Sender write(@NonNull Output output, @NonNull Callback callback) {
    fromOutput(response, toJettyCallback(ctx, callback), output).send(false);
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
