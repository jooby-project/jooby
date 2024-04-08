/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Server;
import io.jooby.ServerSentEmitter;
import io.jooby.ServerSentMessage;
import io.jooby.SneakyThrows;
import io.jooby.netty.buffer.NettyDataBuffer;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class NettyServerSentEmitter implements ServerSentEmitter, GenericFutureListener {

  private Logger log = LoggerFactory.getLogger(ServerSentEmitter.class);

  private final NettyContext netty;

  private String id;

  private AtomicBoolean open = new AtomicBoolean(true);

  private SneakyThrows.Runnable closeTask;

  public NettyServerSentEmitter(NettyContext netty) {
    this.netty = netty;
    this.id = UUID.randomUUID().toString();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public boolean isOpen() {
    return open.get();
  }

  @Override
  public ServerSentEmitter setId(String id) {
    this.id = id;
    return this;
  }

  @NonNull @Override
  public Context getContext() {
    return Context.readOnly(netty);
  }

  @NonNull @Override
  public ServerSentEmitter send(ServerSentMessage data) {
    if (checkOpen()) {
      // TODO: FIX usage of new DataBuffer
      var nettyDataBuffer = (NettyDataBuffer) data.toByteArray(netty);
      netty.ctx.writeAndFlush(nettyDataBuffer.getNativeBuffer()).addListener(this);
    } else {
      log.warn("server-sent-event closed: {}", id);
    }
    return this;
  }

  @Override
  public ServerSentEmitter keepAlive(long timeInMillis) {
    if (checkOpen()) {
      EventLoop executor = netty.ctx.channel().eventLoop().next();
      executor.schedule(new KeepAlive(this, timeInMillis), timeInMillis, TimeUnit.MILLISECONDS);
    }
    return this;
  }

  @Override
  public void onClose(SneakyThrows.Runnable task) {
    this.closeTask = task;
  }

  @NonNull @Override
  public void close() {
    if (open.compareAndSet(true, false)) {
      try {
        if (closeTask != null) {
          log.debug("running close task on sse {}", id);
          closeTask.run();
        }
      } finally {
        log.debug("closing sse {}", id);
        netty.ctx.close();
      }
    }
  }

  @Override
  public void operationComplete(Future future) throws Exception {
    if (!future.isSuccess()) {
      if (Server.connectionLost(future.cause())) {
        close();
      } else {
        log.error(
            "server-sent-event resulted in exception: id {} {}",
            id,
            netty.getRequestPath(),
            future.cause());
        if (SneakyThrows.isFatal(future.cause())) {
          throw SneakyThrows.propagate(future.cause());
        }
      }
    }
  }

  private boolean checkOpen() {
    if (isOpen()) {
      // client might be close the channel:
      if (netty.ctx.channel().isOpen()) {
        return true;
      } else {
        close();
        return false;
      }
    }
    return false;
  }
}
