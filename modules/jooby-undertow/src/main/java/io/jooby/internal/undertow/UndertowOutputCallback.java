/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import io.jooby.output.Output;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;

public class UndertowOutputCallback implements IoCallback {

  private Iterator<ByteBuffer> iterator;
  private IoCallback callback;

  public UndertowOutputCallback(Output buffer, IoCallback callback) {
    this.iterator = buffer.iterator();
    this.callback = callback;
  }

  public void send(HttpServerExchange exchange) {
    exchange.getResponseSender().send(iterator.next(), this);
  }

  @Override
  public void onComplete(HttpServerExchange exchange, Sender sender) {
    if (iterator.hasNext()) {
      sender.send(iterator.next(), this);
    } else {
      callback.onComplete(exchange, sender);
    }
  }

  @Override
  public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
    callback.onException(exchange, sender, exception);
  }
}
