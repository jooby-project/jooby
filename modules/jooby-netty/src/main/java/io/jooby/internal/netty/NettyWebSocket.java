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
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class NettyWebSocket implements WebSocketConfigurer, WebSocket {
  /** All connected websocket. */
  private static final ConcurrentMap<String, List<WebSocket>> all = new ConcurrentHashMap<>();

  private final NettyContext netty;
  private final boolean dispatch;
  private final String key;
  private ByteBuf buffer;
  private WebSocket.OnConnect connectCallback;
  private WebSocket.OnMessage messageCallback;
  private OnClose onCloseCallback;
  private OnError onErrorCallback;

  public NettyWebSocket(NettyContext ctx) {
    this.netty = ctx;
    this.key = ctx.getRoute().getPattern();
    this.dispatch = !ctx.isInIoThread();
  }

  public WebSocket send(String text, boolean broadcast) {
    if (broadcast) {
      for (WebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
        ws.send(text, false);
      }
    } else {
      send(new TextWebSocketFrame(text));
    }
    return this;
  }

  public WebSocket send(byte[] bytes, boolean broadcast) {
    if (broadcast) {
      for (WebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
        ws.send(bytes, false);
      }
    } else {
      send(new TextWebSocketFrame(Unpooled.wrappedBuffer(bytes)));
    }
    return this;
  }

  @Override public WebSocket render(Object value, boolean broadcast) {
    if (broadcast) {
      for (WebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
        ws.render(value, false);
      }
    } else {
      Context.websocket(netty, this).render(value);
    }
    return this;
  }

  private WebSocket send(TextWebSocketFrame frame) {
    if (isOpen()) {
      netty.ctx.channel().writeAndFlush(frame);
    } else {
      handleError(new IllegalStateException("Attempt to send a message on closed web socket"));
    }
    return this;
  }

  @Override public Context getContext() {
    return Context.readOnly(netty);
  }

  @Nonnull @Override public List<WebSocket> getSessions() {
    List<WebSocket> sessions = all.get(key);
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

        fireCallback(webSocketTask(() -> messageCallback.onMessage(this, message)));
      } else {
        buffer = Unpooled.copiedBuffer(frame.content());
      }
    } finally {
      frame.release();
    }
  }

  private void handleClose(WebSocketCloseStatus closeStatus) {
    try {
      if (isOpen()) {
        if (onCloseCallback != null) {
          Runnable task = webSocketTask(() -> onCloseCallback.onClose(this, closeStatus));
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
      netty.getRouter().getLog().error("WS {} resulted in exception", netty.pathString(), x);
    } else {
      onErrorCallback.onError(this, x);
    }

    if (SneakyThrows.isFatal(x)) {
      throw SneakyThrows.propagate(x);
    }
  }

  void fireConnect() {
    addSession(this);
    if (connectCallback != null) {
      fireCallback(webSocketTask(() -> connectCallback.onConnect(this)));
    }
  }

  private Runnable webSocketTask(Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Throwable x) {
        handleError(x);
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
    List<WebSocket> sockets = all.get(ws.key);
    if (sockets != null) {
      sockets.remove(ws);
    }
  }
}
