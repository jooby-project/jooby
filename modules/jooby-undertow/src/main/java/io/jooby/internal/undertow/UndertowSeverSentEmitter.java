/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioIoThread;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Server;
import io.jooby.ServerSentEmitter;
import io.jooby.ServerSentMessage;
import io.jooby.SneakyThrows;

public class UndertowSeverSentEmitter
    implements ServerSentEmitter, UndertowServerSentConnection.EventCallback {
  private UndertowServerSentConnection connection;

  private Logger log = LoggerFactory.getLogger(ServerSentEmitter.class);

  private UndertowContext context;

  private String id;

  private AtomicBoolean open = new AtomicBoolean(true);

  private SneakyThrows.Runnable closeTask;

  public UndertowSeverSentEmitter(UndertowContext context) {
    this.context = context;
    this.id = UUID.randomUUID().toString();
    this.connection = new UndertowServerSentConnection(context);
  }

  @NonNull @Override
  public Context getContext() {
    return Context.readOnly(context);
  }

  @NonNull @Override
  public ServerSentEmitter send(ServerSentMessage data) {
    if (checkOpen()) {
      connection.send(data, null);
    } else {
      log.warn("server-sent-event closed: {}", id);
    }
    return this;
  }

  @Override
  public ServerSentEmitter keepAlive(long timeInMillis) {
    if (checkOpen()) {
      XnioIoThread executor = context.exchange.getIoThread();
      executor.executeAfter(new KeepAlive(this, timeInMillis), timeInMillis, TimeUnit.MILLISECONDS);
    }
    return this;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public ServerSentEmitter setId(String id) {
    this.id = id;
    return this;
  }

  @Override
  public boolean isOpen() {
    return open.get();
  }

  @Override
  public void onClose(SneakyThrows.Runnable task) {
    this.closeTask = task;
  }

  @NonNull @Override
  public void close() {
    if (open.compareAndSet(true, false)) {
      if (closeTask != null) {
        try {
          closeTask.run();
        } finally {
          try {
            connection.close();
          } catch (IOException cause) {
            log.error(
                "server-sent-event resulted in exception: id {} {}",
                id,
                context.getRequestPath(),
                cause);
          }
        }
      }
    }
  }

  @Override
  public void done(UndertowServerSentConnection connection, ServerSentMessage message) {
    log.debug(
        "server-sent-event {} message sent id: {}, event: {}, data: {}",
        id,
        message.getId(),
        message.getEvent(),
        message.getData());
  }

  @Override
  public void failed(
      UndertowServerSentConnection connection, ServerSentMessage message, Throwable exception) {
    if (Server.connectionLost(exception)) {
      close();
    } else {
      log.error(
          "server-sent-event resulted in exception: id {} {}",
          id,
          context.getRequestPath(),
          exception);
      if (SneakyThrows.isFatal(exception)) {
        throw SneakyThrows.propagate(exception);
      }
    }
  }

  private boolean checkOpen() {
    if (isOpen()) {
      // client might be close the channel:
      if (context.exchange.isComplete()) {
        close();
        return false;
      } else {
        return true;
      }
    }
    return false;
  }
}
