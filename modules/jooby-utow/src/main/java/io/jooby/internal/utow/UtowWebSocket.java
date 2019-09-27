package io.jooby.internal.utow;

import io.jooby.Context;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketConfigurer;
import io.jooby.WebSocketMessage;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class UtowWebSocket extends AbstractReceiveListener
    implements WebSocketConfigurer, WebSocket, WebSocketCallback<Void> {

  /** All connected websocket. */
  private static final ConcurrentMap<String, List<WebSocket>> all = new ConcurrentHashMap<>();

  private final UtowContext ctx;
  private final WebSocketChannel channel;
  private OnConnect onConnectCallback;
  private OnMessage onMessageCallback;
  private OnClose onCloseCallback;
  private OnError onErrorCallback;
  private String key;

  public UtowWebSocket(UtowContext ctx, WebSocketChannel channel) {
    this.ctx = ctx;
    this.channel = channel;
    this.key = ctx.getRoute().getPattern();
  }

  @Override protected long getMaxTextBufferSize() {
    return MAX_BUFFER_SIZE;
  }

  @Nonnull @Override public Context getContext() {
    return Context.readOnly(ctx);
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

  @Override public boolean isOpen() {
    return channel.isOpen();
  }

  @Nonnull @Override public WebSocket send(@Nonnull String message, boolean broadcast) {
    return send(message.getBytes(StandardCharsets.UTF_8), broadcast);
  }

  @Nonnull @Override public WebSocket send(@Nonnull byte[] message, boolean broadcast) {
    if (broadcast) {
      for (WebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
        ws.send(message, false);
      }
    } else {
      if (isOpen()) {
        try {
          WebSockets.sendText(ByteBuffer.wrap(message), channel, this);
        } catch (Throwable x) {
          onError(channel, x);
        }
      } else {
        onError(channel, new IllegalStateException("Attempt to send a message on closed web socket"));
      }
    }
    return this;
  }

  @Nonnull @Override public WebSocket render(@Nonnull Object value, boolean broadcast) {
    if (broadcast) {
      for (WebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
        ws.render(value, false);
      }
    } else {
      try {
        Context.websocket(ctx, this).render(value);
      } catch (Throwable x) {
        onError(channel, x);
      }
    }
    return this;
  }

  @Nonnull @Override public WebSocket close(@Nonnull WebSocketCloseStatus closeStatus) {
    handleClose(closeStatus);
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onConnect(@Nonnull OnConnect callback) {
    onConnectCallback = callback;
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onMessage(@Nonnull OnMessage callback) {
    onMessageCallback = callback;
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onError(@Nonnull OnError callback) {
    onErrorCallback = callback;
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onClose(@Nonnull OnClose callback) {
    onCloseCallback = callback;
    return this;
  }

  void fireConnect() {
    try {
      addSession(this);
      channel.getReceiveSetter().set(this);
      channel.resumeReceives();
      if (onConnectCallback != null) {
        onConnectCallback.onConnect(this);
      }
    } catch (Throwable x) {
      onError(channel, x);
    }
  }

  @Override protected void onFullTextMessage(WebSocketChannel channel,
      BufferedTextMessage message) throws IOException {
    if (onMessageCallback != null) {
      try {
        onMessageCallback.onMessage(this, WebSocketMessage.create(getContext(), message.getData()));
      } catch (Throwable x) {
        onError(channel, x);
      }
    }
  }

  @Override protected void onError(WebSocketChannel channel, Throwable x) {
    // should close?
    if (Server.connectionLost(x) || SneakyThrows.isFatal(x)) {
      handleClose(WebSocketCloseStatus.SERVER_ERROR);
    }

    if (onErrorCallback == null) {
      ctx.getRouter().getLog().error("WS {} resulted in exception", ctx.pathString(), x);
    } else {
      onErrorCallback.onError(this, x);
    }

    if (SneakyThrows.isFatal(x)) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override protected void onCloseMessage(CloseMessage cm,
      WebSocketChannel channel) {
    handleClose(WebSocketCloseStatus.valueOf(cm.getCode())
        .orElseGet(() -> new WebSocketCloseStatus(cm.getCode(), cm.getReason())));
  }

  private void handleClose(WebSocketCloseStatus closeStatus) {
    try {
      if (onCloseCallback != null) {
        onCloseCallback.onClose(this, closeStatus);
      }
    } catch (Throwable x) {
      onError(channel, x);
    } finally {
      removeSession(this);
    }
  }

  private void addSession(UtowWebSocket ws) {
    all.computeIfAbsent(ws.key, k -> new CopyOnWriteArrayList<>()).add(ws);
  }

  private void removeSession(UtowWebSocket ws) {
    List<WebSocket> sockets = all.get(ws.key);
    if (sockets != null) {
      sockets.remove(ws);
    }
  }

  @Override public void complete(WebSocketChannel channel, Void context) {
    // NOOP
  }

  @Override public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
    ctx.getRouter().getLog().error("WebSocket.send resulted in exception", throwable);
  }
}
