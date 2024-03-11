/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Server;
import io.jooby.ServerSentEmitter;
import io.jooby.ServerSentMessage;
import io.jooby.SneakyThrows;

public class JettyServerSentEmitter implements ServerSentEmitter, Callback {
  private JettyContext jetty;

  private Response response;

  private AtomicBoolean open = new AtomicBoolean(true);

  private String id;

  private SneakyThrows.Runnable closeTask;

  public JettyServerSentEmitter(JettyContext jetty, Response response) {
    this.jetty = jetty;
    this.response = response;
    this.id = UUID.randomUUID().toString();
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

  @NonNull @Override
  public Context getContext() {
    return Context.readOnly(jetty);
  }

  @NonNull @Override
  public ServerSentEmitter send(ServerSentMessage data) {
    if (isOpen()) {
      response.write(false, ByteBuffer.wrap(data.toByteArray(jetty)), this);
    }
    return this;
  }

  @Override
  public void failed(Throwable failure) {
    if (Server.connectionLost(failure) || failure instanceof EofException) {
      close();
    } else {
      jetty.failed(failure);
    }
  }

  @Override
  public ServerSentEmitter keepAlive(long timeInMillis) {
    if (isOpen()) {
      var scheduler = jetty.request.getConnectionMetaData().getConnector().getScheduler();
      scheduler.schedule(new KeepAlive(this, timeInMillis), timeInMillis, TimeUnit.MILLISECONDS);
    }
    return this;
  }

  @Override
  public void onClose(SneakyThrows.Runnable task) {
    this.closeTask = task;
  }

  @Override
  public void close() {
    if (open.compareAndSet(true, false)) {
      try {
        if (closeTask != null) {
          closeTask.run();
        }
      } finally {
        response.write(true, null, jetty);
      }
    }
  }
}
