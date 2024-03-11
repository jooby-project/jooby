/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class JettyCallbacks {

  public static Callback fromByteBufferArray(
      Response response, Callback callback, ByteBuffer[] buffers) {
    return fromByteBufferArray(response, callback, 0, buffers);
  }

  private static Callback fromByteBufferArray(
      Response response, Callback callback, int index, ByteBuffer[] buffers) {
    return new Callback() {
      @Override
      public void succeeded() {
        if (index == buffers.length - 1) {
          response.write(true, buffers[index], callback);
        } else {
          response.write(
              false, buffers[index], fromByteBufferArray(response, callback, index + 1, buffers));
        }
      }

      @Override
      public void failed(Throwable x) {
        callback.failed(x);
      }
    };
  }
}
