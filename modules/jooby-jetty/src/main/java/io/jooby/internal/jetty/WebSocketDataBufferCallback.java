/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;
import java.util.Iterator;

import org.eclipse.jetty.websocket.api.Callback;

import io.jooby.SneakyThrows.Consumer2;
import io.jooby.output.Output;

public class WebSocketDataBufferCallback implements Callback {

  private final Iterator<ByteBuffer> it;
  private final Callback cb;
  private Consumer2<ByteBuffer, Callback> sender;

  public WebSocketDataBufferCallback(
      Callback cb, Output buffer, Consumer2<ByteBuffer, Callback> sender) {
    this.cb = cb;
    this.it = buffer.iterator();
    this.sender = sender;
  }

  public void send() {
    if (it.hasNext()) {
      sender.accept(it.next(), this);
    }
  }

  @Override
  public void succeed() {
    try {
      cb.succeed();
    } finally {
      send();
    }
  }

  @Override
  public void fail(Throwable x) {
    cb.fail(x);
  }
}
