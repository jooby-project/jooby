/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;
import java.util.Iterator;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import io.jooby.output.Output;

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

  public static class OutputCallback implements Callback {

    private final Response response;
    private final Callback cb;
    private final Iterator<ByteBuffer> it;
    private boolean closeOnLast;

    public OutputCallback(Response response, Callback cb, Output buffer) {
      this.response = response;
      this.cb = cb;
      this.it = buffer.iterator();
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
      response.write(last, buffer, cb);
    }

    @Override
    public void succeeded() {
      send(closeOnLast);
    }

    @Override
    public void failed(Throwable x) {
      cb.failed(x);
    }
  }

  public static OutputCallback fromOutput(Response response, Callback cb, Output output) {
    return new OutputCallback(response, cb, output);
  }

  public static ByteBufferArrayCallback fromByteBufferArray(
      Response response, Callback cb, ByteBuffer[] buffer) {
    return new ByteBufferArrayCallback(response, cb, buffer);
  }
}
