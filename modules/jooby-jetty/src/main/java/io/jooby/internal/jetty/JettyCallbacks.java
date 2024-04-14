/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import io.jooby.buffer.DataBuffer;

public class JettyCallbacks {
  public static class ByteBufferArrayCallback implements Callback {
    private Response response;
    private Callback callback;
    private int index;
    private ByteBuffer[] buffers;

    public ByteBufferArrayCallback(Response response, Callback callback, ByteBuffer[] buffers) {
      this.response = response;
      this.callback = callback;
      this.buffers = buffers;
    }

    public void send() {
      if (index == buffers.length - 1) {
        response.write(true, buffers[index], callback);
      } else {
        response.write(false, buffers[index++], this);
      }
    }

    @Override
    public void succeeded() {
      send();
    }

    @Override
    public void failed(Throwable x) {
      callback.failed(x);
    }
  }

  public static class DataBufferCallback implements Callback {

    private final Response response;
    private final Callback cb;
    private final DataBuffer.ByteBufferIterator it;
    private boolean closeOnLast;

    public DataBufferCallback(Response response, Callback cb, DataBuffer buffer) {
      this.response = response;
      this.cb = cb;
      this.it = buffer.readableByteBuffers();
    }

    public void send(boolean closeOnLast) {
      this.closeOnLast = closeOnLast;
      if (it.hasNext()) {
        var buffer = it.next();
        if (it.hasNext()) {
          response.write(false, buffer, this);
        } else {
          sendLast(closeOnLast, buffer);
        }
      } else {
        sendLast(closeOnLast, null);
      }
    }

    private void sendLast(boolean last, ByteBuffer buffer) {
      try {
        response.write(last, buffer, cb);
      } finally {
        it.close();
      }
    }

    @Override
    public void succeeded() {
      send(closeOnLast);
    }

    @Override
    public void failed(Throwable x) {
      try {
        cb.failed(x);
      } finally {
        it.close();
      }
    }
  }

  public static DataBufferCallback fromDataBuffer(
      Response response, Callback cb, DataBuffer buffer) {
    return new DataBufferCallback(response, cb, buffer);
  }

  public static ByteBufferArrayCallback fromByteBufferArray(
      Response response, Callback cb, ByteBuffer[] buffer) {
    return new ByteBufferArrayCallback(response, cb, buffer);
  }
}
