/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import io.jooby.Context;
import io.jooby.Server;
import io.jooby.ServerSentEmitter;
import io.jooby.ServerSentMessage;
import io.jooby.SneakyThrows;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioIoThread;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UtowSeverSentEmitter implements ServerSentEmitter, IoCallback {
  private Logger log = LoggerFactory.getLogger(ServerSentEmitter.class);

  private final UtowContext utow;

  private String id;

  private AtomicBoolean open = new AtomicBoolean(true);

  private SneakyThrows.Runnable closeTask;

  public UtowSeverSentEmitter(UtowContext utow) {
    this.utow = utow;
    this.id = UUID.randomUUID().toString();
  }

  @Nonnull @Override public Context getContext() {
    return Context.readOnly(utow);
  }

  @Nonnull @Override public ServerSentEmitter send(ServerSentMessage data) {
    if (checkOpen()) {
      Sender sender = utow.exchange.getResponseSender();
      sender.send(ByteBuffer.wrap(data.toByteArray(utow)), this);
    } else {
      log.warn("server-sent-event closed: {}", id);
    }
    return this;
  }

  @Override public ServerSentEmitter keepAlive(long timeInMillis) {
    if (checkOpen()) {
      XnioIoThread executor = utow.exchange.getIoThread();
      executor.executeAfter(new KeepAlive(this, timeInMillis), timeInMillis, TimeUnit.MILLISECONDS);
    }
    return this;
  }

  @Override public String getId() {
    return id;
  }

  @Override public ServerSentEmitter setId(String id) {
    this.id = id;
    return this;
  }

  @Override public boolean isOpen() {
    return open.get();
  }

  @Override public void onClose(SneakyThrows.Runnable task) {
    this.closeTask = task;
  }

  @Nonnull @Override public void close() {
    if (open.compareAndSet(true, false)) {
      if (closeTask != null) {
        try {
          closeTask.run();
        } finally {
          utow.exchange.endExchange();
        }
      }
    }
  }

  @Override public void onComplete(HttpServerExchange exchange, Sender sender) {
  }

  @Override
  public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
    if (Server.connectionLost(exception)) {
      close();
    } else {
      log.error("server-sent-event resulted in exception: id {} {}", id, utow.getRequestPath(),
          exception);
      if (SneakyThrows.isFatal(exception)) {
        throw SneakyThrows.propagate(exception);
      }
    }
  }

  private boolean checkOpen() {
    if (isOpen()) {
      // client might be close the channel:
      if (utow.exchange.isComplete()) {
        close();
        return false;
      } else {
        return true;
      }
    }
    return false;
  }
}
