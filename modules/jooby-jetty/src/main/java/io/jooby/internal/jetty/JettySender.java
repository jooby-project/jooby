/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Sender;
import io.jooby.output.Output;

public class JettySender implements Sender {
  private final JettyContext ctx;
  private final Response response;
  private HttpFields.Mutable trailers;
  private ByteBuffer pending;
  private org.eclipse.jetty.util.Callback pendingCallback;

  public JettySender(JettyContext ctx) {
    this.ctx = ctx;
    this.response = ctx.response;
    this.trailers = ctx.trailers;
  }

  @Override
  public Sender setTrailer(@NonNull String name, @NonNull String value) {
    if (trailers == null) {
      trailers = HttpFields.build();
    }
    trailers.put(name, value);
    return this;
  }

  @Override
  public Sender write(@NonNull byte[] data, @NonNull Callback callback) {
    return write(ByteBuffer.wrap(data), callback);
  }

  @NonNull @Override
  public Sender write(@NonNull Output output, @NonNull Callback callback) {
    return write(output.asByteBuffer(), callback);
  }

  public Sender write(@NonNull ByteBuffer buffer, @NonNull Callback callback) {
    response.write(false, buffer, toJettyCallback(ctx, callback));
    //    if (trailers == null) {
    //      response.write(false, buffer, toJettyCallback(ctx, callback));
    //    } else {
    //      if (pending != null) {
    //        response.write(false, pending, pendingCallback);
    //      }
    //      pending = buffer;
    //      pendingCallback = toJettyCallback(ctx, callback);
    //    }
    return this;
  }

  @Override
  public void close() {
    if (trailers != null) {
      response.setTrailersSupplier(() -> trailers);
      response.write(true, null, ctx);
    }
    //    if (pending != null) {
    //      response.setTrailersSupplier(() -> trailers);
    //      response.write(true, pending, ctx);
    //    } else {
    //      response.write(true, null, ctx);
    //    }
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
}
