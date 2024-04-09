/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketConfigurer;
import io.jooby.WebSocketMessage;
import io.jooby.buffer.DataBuffer;
import io.jooby.netty.buffer.NettyDataBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;

public class NettyWebSocket implements WebSocketConfigurer, WebSocket {
  /** All connected websocket. */
  private static class WriteCallbackAdaptor implements ChannelFutureListener {

    private NettyWebSocket ws;

    private WebSocket.WriteCallback callback;

    public WriteCallbackAdaptor(NettyWebSocket ws, WriteCallback callback) {
      this.ws = ws;
      this.callback = callback;
    }

    @Override
    public void operationComplete(ChannelFuture future) {
      Throwable cause = future.cause();
      try {
        if (cause != null) {
          if (Server.connectionLost(cause)) {
            ws.netty
                .getRouter()
                .getLog()
                .debug(
                    "WebSocket {} send method resulted in exception",
                    ws.getContext().getRequestPath(),
                    cause);
          } else {
            ws.netty
                .getRouter()
                .getLog()
                .error(
                    "WebSocket {} send method resulted in exception",
                    ws.getContext().getRequestPath(),
                    cause);
          }
        }
      } finally {
        callback.operationComplete(ws, cause);
      }
    }
  }

  public static final ConcurrentMap<String, List<NettyWebSocket>> all = new ConcurrentHashMap<>();

  static final AttributeKey<NettyWebSocket> WS =
      AttributeKey.newInstance(NettyWebSocket.class.getName());

  private final NettyContext netty;
  private final boolean dispatch;
  private final String key;
  private ByteBuf buffer;
  private WebSocket.OnConnect connectCallback;
  private WebSocket.OnMessage messageCallback;
  private AtomicReference<OnClose> onCloseCallback = new AtomicReference<>();
  private OnError onErrorCallback;
  private CountDownLatch ready = new CountDownLatch(1);
  private AtomicBoolean open = new AtomicBoolean(false);

  public NettyWebSocket(NettyContext ctx) {
    this.netty = ctx;
    this.key = ctx.getRoute().getPattern();
    this.dispatch = !ctx.isInIoThread();
    this.netty.ctx.channel().attr(WS).set(this);
  }

  @NonNull @Override
  public WebSocket send(@NonNull String message, @NonNull WriteCallback callback) {
    return sendMessage(Unpooled.copiedBuffer(message, StandardCharsets.UTF_8), false, callback);
  }

  @NonNull @Override
  public WebSocket send(byte[] bytes, @NonNull WriteCallback callback) {
    return sendMessage(Unpooled.wrappedBuffer(bytes), false, callback);
  }

  @NonNull @Override
  public WebSocket send(@NonNull ByteBuffer message, @NonNull WriteCallback callback) {
    return sendMessage(Unpooled.wrappedBuffer(message), false, callback);
  }

  @NonNull @Override
  public WebSocket sendBinary(@NonNull ByteBuffer message, @NonNull WriteCallback callback) {
    return sendMessage(Unpooled.wrappedBuffer(message), true, callback);
  }

  @NonNull @Override
  public WebSocket sendBinary(@NonNull String message, @NonNull WriteCallback callback) {
    return sendMessage(Unpooled.copiedBuffer(message, StandardCharsets.UTF_8), true, callback);
  }

  @NonNull @Override
  public WebSocket sendBinary(@NonNull byte[] message, @NonNull WriteCallback callback) {
    return sendMessage(Unpooled.wrappedBuffer(message), true, callback);
  }

  @NonNull @Override
  public WebSocket sendBinary(@NonNull DataBuffer message, @NonNull WriteCallback callback) {
    return sendMessage(((NettyDataBuffer) message).getNativeBuffer(), true, callback);
  }

  @NonNull @Override
  public WebSocket send(@NonNull DataBuffer message, @NonNull WriteCallback callback) {
    return sendMessage(((NettyDataBuffer) message).getNativeBuffer(), false, callback);
  }

  @Override
  public WebSocket render(Object value, @NonNull WriteCallback callback) {
    return renderMessage(value, false, callback);
  }

  @Override
  public WebSocket renderBinary(Object value, @NonNull WriteCallback callback) {
    return renderMessage(value, true, callback);
  }

  private WebSocket renderMessage(Object value, boolean binary, WriteCallback callback) {
    try {
      Context.websocket(netty, this, binary, callback).render(value);
    } catch (Throwable x) {
      handleError(x);
    }
    return this;
  }

  private WebSocket sendMessage(ByteBuf buffer, boolean binary, WriteCallback callback) {
    if (isOpen()) {
      WebSocketFrame frame =
          binary ? new BinaryWebSocketFrame(buffer) : new TextWebSocketFrame(buffer);
      netty
          .ctx
          .channel()
          .writeAndFlush(frame)
          .addListener(new WriteCallbackAdaptor(this, callback));
    } else {
      handleError(new IllegalStateException("Attempt to send a message on closed web socket"));
    }
    return this;
  }

  @Override
  public Context getContext() {
    return Context.readOnly(netty);
  }

  @NonNull @Override
  public List<WebSocket> getSessions() {
    List<NettyWebSocket> sessions = all.get(key);
    if (sessions == null) {
      return Collections.emptyList();
    }
    List<WebSocket> result = new ArrayList<>(sessions);
    result.remove(this);
    return result;
  }

  public boolean isOpen() {
    return open.get() && netty.ctx.channel().isOpen();
  }

  @Override
  public void forEach(SneakyThrows.Consumer<WebSocket> callback) {
    for (NettyWebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
      try {
        callback.accept(ws);
      } catch (Exception cause) {
        netty
            .getRouter()
            .getLog()
            .debug("Broadcast of: {} resulted in exception", netty.getRequestPath(), cause);
      }
    }
  }

  @Override
  public WebSocketConfigurer onConnect(WebSocket.OnConnect callback) {
    connectCallback = callback;
    return this;
  }

  @Override
  public WebSocketConfigurer onMessage(WebSocket.OnMessage callback) {
    messageCallback = callback;
    return this;
  }

  @Override
  public WebSocketConfigurer onClose(WebSocket.OnClose callback) {
    onCloseCallback.set(callback);
    return this;
  }

  @Override
  public WebSocketConfigurer onError(OnError callback) {
    onErrorCallback = callback;
    return this;
  }

  @Override
  public WebSocket close(WebSocketCloseStatus closeStatus) {
    handleClose(closeStatus);
    return this;
  }

  void handleFrame(WebSocketFrame frame) {
    waitForConnect();
    try {
      if (frame instanceof TextWebSocketFrame
          || frame instanceof BinaryWebSocketFrame
          || frame instanceof ContinuationWebSocketFrame) {
        handleMessage(frame);
      } else if (frame instanceof PingWebSocketFrame) {
        netty
            .ctx
            .channel()
            .writeAndFlush(new PongWebSocketFrame(frame.content()))
            .addListener(new WriteCallbackAdaptor(this, WriteCallback.NOOP));
      } else if (frame instanceof CloseWebSocketFrame) {
        handleClose(toWebSocketCloseStatus((CloseWebSocketFrame) frame));
      }
    } catch (Throwable x) {
      handleError(x);
    }
  }

  private void handleMessage(WebSocketFrame frame) {
    try {
      if (messageCallback != null) {
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
      }
    } finally {
      frame.release();
    }
  }

  private void handleClose(WebSocketCloseStatus closeStatus) {
    OnClose callback = onCloseCallback.getAndSet(null);
    if (isOpen()) {
      open.set(false);
      // close socket:
      netty
          .ctx
          .channel()
          .writeAndFlush(new CloseWebSocketFrame(closeStatus.getCode(), closeStatus.getReason()))
          .addListener(ChannelFutureListener.CLOSE);
    }
    try {
      if (callback != null) {
        // fire callback:
        fireCallback(webSocketTask(() -> callback.onClose(this, closeStatus), false));
      }
    } finally {
      // clear from active sessions:
      this.netty.ctx.channel().attr(WS).set(null);
      removeSession(this);
    }
  }

  private void handleError(Throwable x) {
    // should close?
    if (Server.connectionLost(x) || SneakyThrows.isFatal(x)) {
      handleClose(WebSocketCloseStatus.SERVER_ERROR);
    }

    if (onErrorCallback == null) {
      netty
          .getRouter()
          .getLog()
          .error("Websocket resulted in exception: {}", netty.getRequestPath(), x);
    } else {
      onErrorCallback.onError(this, x);
    }

    if (SneakyThrows.isFatal(x)) {
      this.netty.ctx.channel().attr(WS).set(null);
      throw SneakyThrows.propagate(x);
    }
  }

  void fireConnect() {
    open.set(true);
    addSession(this);
    if (connectCallback != null) {
      fireCallback(
          webSocketTask(
              () -> {
                connectCallback.onConnect(this);
              },
              true));
    } else {
      ready.countDown();
    }
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
    byte[] bytes = new byte[buffer.readableBytes()];
    buffer.getBytes(0, bytes);
    return bytes;
  }

  private static WebSocketCloseStatus toWebSocketCloseStatus(CloseWebSocketFrame frame) {
    try {
      return WebSocketCloseStatus.valueOf(frame.statusCode())
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

  private void waitForConnect() {
    try {
      ready.await();
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }
  }
}
