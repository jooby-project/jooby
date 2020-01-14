/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketConfigurer;
import io.jooby.WebSocketMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NettyWebSocket implements WebSocketConfigurer, WebSocket, ChannelFutureListener {
  /** All connected websocket. */
  private static final ConcurrentMap<String, List<NettyWebSocket>> all = new ConcurrentHashMap<>();

  static final AttributeKey<NettyWebSocket> WS = AttributeKey
      .newInstance(NettyWebSocket.class.getName());

  private final NettyContext netty;
  private final boolean dispatch;
  private final String key;
  private ByteBuf buffer;
  private WebSocket.OnConnect connectCallback;
  private WebSocket.OnMessage messageCallback;
  private OnClose onCloseCallback;
  private OnError onErrorCallback;
  private CountDownLatch ready = new CountDownLatch(1);

  public NettyWebSocket(NettyContext ctx) {
    this.netty = ctx;
    this.key = ctx.getRoute().getPattern();
    this.dispatch = !ctx.isInIoThread();
    this.netty.ctx.channel().attr(WS).set(this);
  }

  public WebSocket send(String text, boolean broadcast) {
    return send(Unpooled.copiedBuffer(text, StandardCharsets.UTF_8), broadcast);
  }

  public WebSocket send(byte[] bytes, boolean broadcast) {
    return send(Unpooled.wrappedBuffer(bytes), broadcast);
  }

  @Override public WebSocket render(Object value, boolean broadcast) {
    if (broadcast) {
      for (WebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
        ws.render(value, false);
      }
    } else {
      try {
        Context.websocket(netty, this).render(value);
      } catch (Throwable x) {
        handleError(x);
      }
    }
    return this;
  }

  private WebSocket send(ByteBuf buffer, boolean broadcast) {
    if (broadcast) {
      for (NettyWebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
        ws.send(buffer, false);
      }
    } else {
      if (isOpen()) {
        netty.ctx.channel().writeAndFlush(new TextWebSocketFrame(buffer)).addListener(this);
      } else {
        handleError(new IllegalStateException("Attempt to send a message on closed web socket"));
      }
    }
    return this;
  }

  @Override public Context getContext() {
    return Context.readOnly(netty);
  }

  @Nonnull @Override public List<WebSocket> getSessions() {
    List<NettyWebSocket> sessions = all.get(key);
    if (sessions == null) {
      return Collections.emptyList();
    }
    List<WebSocket> result = new ArrayList<>(sessions);
    result.remove(this);
    return result;
  }

  public boolean isOpen() {
    return netty.ctx.channel().isOpen();
  }

  @Override public WebSocketConfigurer onConnect(WebSocket.OnConnect callback) {
    connectCallback = callback;
    return this;
  }

  @Override public WebSocketConfigurer onMessage(WebSocket.OnMessage callback) {
    messageCallback = callback;
    return this;
  }

  @Override public WebSocketConfigurer onClose(WebSocket.OnClose callback) {
    onCloseCallback = callback;
    return this;
  }

  @Override public WebSocketConfigurer onError(OnError callback) {
    onErrorCallback = callback;
    return this;
  }

  @Override public WebSocket close(WebSocketCloseStatus closeStatus) {
    handleClose(closeStatus);
    return this;
  }

  void handleFrame(WebSocketFrame frame) {
    waitForConnect();
    try {
      if (frame instanceof TextWebSocketFrame || frame instanceof BinaryWebSocketFrame
          || frame instanceof ContinuationWebSocketFrame) {
        handleMessage(frame);
      } else if (frame instanceof CloseWebSocketFrame) {
        handleClose(toWebSocketCloseStatus((CloseWebSocketFrame) frame));
      }
    } catch (Throwable x) {
      handleError(x);
    }
  }

  private void handleMessage(WebSocketFrame frame) {
    try {
      if (frame.isFinalFragment()) {
        ByteBuf content;
        if (buffer != null) {
          buffer.writeBytes(frame.content());
          content = buffer;
          buffer = null;
        } else {
          content = frame.content();
        }
        WebSocketMessage message = WebSocketMessage.create(getContext(), array(content));

        fireCallback(webSocketTask(() -> messageCallback.onMessage(this, message), false));
      } else {
        buffer = Unpooled.copiedBuffer(frame.content());
      }
    } finally {
      frame.release();
    }
  }

  private void waitForConnect() {
    try {
      ready.await(1, TimeUnit.MINUTES);
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }
  }

  private void handleClose(WebSocketCloseStatus closeStatus) {
    try {
      if (isOpen()) {
        if (onCloseCallback != null) {
          Runnable task = webSocketTask(() -> onCloseCallback.onClose(this, closeStatus), false);
          Runnable closeCallback = () -> {
            try {
              task.run();
            } finally {
              netty.ctx.channel()
                  .writeAndFlush(
                      new CloseWebSocketFrame(closeStatus.getCode(), closeStatus.getReason()))
                  .addListener(ChannelFutureListener.CLOSE);
            }
          };
          fireCallback(closeCallback);
        }
      }
    } finally {
      removeSession(this);
    }
  }

  private void handleError(Throwable x) {
    // should close?
    if (Server.connectionLost(x) || SneakyThrows.isFatal(x)) {
      handleClose(WebSocketCloseStatus.SERVER_ERROR);
    }

    if (onErrorCallback == null) {
      netty.getRouter().getLog()
          .error("Websocket resulted in exception: {}", netty.getRequestPath(), x);
    } else {
      onErrorCallback.onError(this, x);
    }

    if (SneakyThrows.isFatal(x)) {
      throw SneakyThrows.propagate(x);
    }
  }

  void fireConnect() {
    // fire only once
    addSession(this);
    if (connectCallback != null) {
      fireCallback(webSocketTask(() -> connectCallback.onConnect(this), true));
    } else {
      ready.countDown();
    }
  }

  private Runnable webSocketTask(Runnable runnable, boolean isInit) {
    return () -> {
      try {
        runnable.run();
      } catch (Throwable x) {
        handleError(x);
      } finally {
        if (isInit) {
          ready.countDown();
        }
      }
    };
  }

  private void fireCallback(Runnable task) {
    if (dispatch) {
      Router router = netty.getRouter();
      router.getWorker().execute(task);
    } else {
      task.run();
    }
  }

  private static byte[] array(ByteBuf buffer) {
    if (buffer.hasArray()) {
      return buffer.array();
    } else {
      byte[] bytes = new byte[buffer.readableBytes()];
      buffer.getBytes(0, bytes);
      return bytes;
    }
  }

  private static WebSocketCloseStatus toWebSocketCloseStatus(CloseWebSocketFrame frame) {
    try {
      return WebSocketCloseStatus
          .valueOf(frame.statusCode())
          .orElseGet(() -> new WebSocketCloseStatus(frame.statusCode(), frame.reasonText()));
    } finally {
      frame.release();
    }
  }

  private void addSession(NettyWebSocket ws) {
    all.computeIfAbsent(ws.key, k -> new CopyOnWriteArrayList<>()).add(ws);
  }

  private void removeSession(NettyWebSocket ws) {
    List<NettyWebSocket> sockets = all.get(ws.key);
    if (sockets != null) {
      sockets.remove(ws);
    }
  }

  @Override public void operationComplete(ChannelFuture future) throws Exception {
    Throwable cause = future.cause();
    if (cause != null) {
      netty.getRouter().getLog().error("WebSocket.send resulted in exception", cause);
    }
  }
}
