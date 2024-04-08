/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.io.IOException;

import io.jooby.SneakyThrows;
import io.jooby.buffer.DataBuffer;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;

public class UndertowDataBufferCallback implements IoCallback {

  private DataBuffer.ByteBufferIterator iterator;
  private IoCallback callback;

  public UndertowDataBufferCallback(DataBuffer buffer, IoCallback callback) {
    this.iterator = buffer.readableByteBuffers();
    this.callback = callback;
  }

  public void send(HttpServerExchange exchange) {
    try {
      exchange.getResponseSender().send(iterator.next(), this);
    } catch (Throwable cause) {
      try {
        iterator.close();
      } finally {
        throw SneakyThrows.propagate(cause);
      }
    }
  }

  @Override
  public void onComplete(HttpServerExchange exchange, Sender sender) {
    if (iterator.hasNext()) {
      sender.send(iterator.next(), this);
    } else {
      try {
        callback.onComplete(exchange, sender);
      } finally {
        iterator.close();
      }
    }
  }

  @Override
  public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
    try {
      callback.onException(exchange, sender, exception);
    } finally {
      iterator.close();
    }
  }
}
