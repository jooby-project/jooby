/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.eclipse.jetty.util.StaticException;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.exceptions.CloseException;

import io.jooby.Context;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketConfigurer;
import io.jooby.WebSocketMessage;

public class JettyWebSocket implements WebSocketListener, WebSocketConfigurer, WebSocket,
    WriteCallback {
  /** All connected websocket. */
  private static final ConcurrentMap<String, List<WebSocket>> all = new ConcurrentHashMap<>();

  private final JettyContext ctx;
  private final String key;
  private final String path;
  private Session session;
  private WebSocket.OnConnect onConnectCallback;
  private WebSocket.OnMessage onMessageCallback;
  private AtomicReference<WebSocket.OnClose> onCloseCallback = new AtomicReference<>();
  private WebSocket.OnError onErrorCallback;
  private AtomicBoolean open = new AtomicBoolean(false);

  public JettyWebSocket(JettyContext ctx) {
    this.ctx = ctx;
    this.path = ctx.getRequestPath();
    this.key = ctx.getRoute().getPattern();
  }

  @Override public void onWebSocketBinary(byte[] payload, int offset, int len) {
  }

  @Override public void onWebSocketText(String message) {
    if (onMessageCallback != null) {
      try {
        onMessageCallback.onMessage(this, WebSocketMessage.create(getContext(), message));
      } catch (Throwable x) {
        onWebSocketError(x);
      }
    }
  }

  @Override public void onWebSocketClose(int statusCode, String reason) {
    if (onCloseCallback != null) {
      handleClose(WebSocketCloseStatus.valueOf(statusCode)
          .orElseGet(() -> new WebSocketCloseStatus(statusCode, reason))
      );
    }
  }

  @Override public void onWebSocketConnect(Session session) {
    try {
      open.set(true);
      this.session = session;
      addSession(this);
      if (onConnectCallback != null) {
        onConnectCallback.onConnect(this);
      }
    } catch (Throwable x) {
      onWebSocketError(x);
    }
  }

  @Override public void onWebSocketError(Throwable x) {
    // should close?
    if (!isTimeout(x)) {
      if (isOpen() && (connectionLost(x) || SneakyThrows.isFatal(x))) {
        handleClose(WebSocketCloseStatus.SERVER_ERROR);
      }

      if (onErrorCallback == null) {
        if (connectionLost(x)) {
          ctx.getRouter().getLog().debug("Websocket resulted in exception: {}", path, x);
        } else {
          ctx.getRouter().getLog().error("Websocket resulted in exception: {}", path, x);
        }
      } else {
        if (!connectionLost(x)) {
          onErrorCallback.onError(this, x);
        }
      }

      if (SneakyThrows.isFatal(x)) {
        throw SneakyThrows.propagate(x);
      }
    }
  }

  private boolean connectionLost(Throwable x) {
    return Server.connectionLost(x) || (x instanceof StaticException &&  x.getMessage().equals("Closed"));
  }

  private boolean isTimeout(Throwable x) {
    if (x instanceof CloseException) {
      Throwable cause = x.getCause();
      return cause instanceof TimeoutException;
    }
    return false;
  }

  @Nonnull @Override public WebSocketConfigurer onConnect(
      @Nonnull WebSocket.OnConnect callback) {
    onConnectCallback = callback;
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onMessage(@Nonnull WebSocket.OnMessage callback) {
    onMessageCallback = callback;
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onError(@Nonnull WebSocket.OnError callback) {
    onErrorCallback = callback;
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onClose(@Nonnull WebSocket.OnClose callback) {
    onCloseCallback.set(callback);
    return this;
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
    return open.get() && session.isOpen();
  }

  @Nonnull @Override public WebSocket send(@Nonnull String message, boolean broadcast) {
    if (broadcast) {
      for (WebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
        ws.send(message, false);
      }
    } else {
      if (isOpen()) {
        try {
          RemoteEndpoint remote = session.getRemote();
          remote.sendString(message, this);
        } catch (Throwable x) {
          onWebSocketError(x);
        }
      } else {
        onWebSocketError(
            new IllegalStateException("Attempt to send a message on closed web socket"));
      }
    }
    return this;
  }

  @Nonnull @Override public WebSocket send(@Nonnull byte[] message, boolean broadcast) {
    return send(new String(message, StandardCharsets.UTF_8), broadcast);
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
        onWebSocketError(x);
      }
    }
    return this;
  }

  @Nonnull @Override public WebSocket close(@Nonnull WebSocketCloseStatus closeStatus) {
    handleClose(closeStatus);
    return this;
  }

  @Override public void writeFailed(Throwable x) {
    if (Server.connectionLost(x)) {
      ctx.getRouter().getLog().debug("Websocket resulted in exception: {}", path, x);
    } else {
      ctx.getRouter().getLog().error("Websocket resulted in exception: {}", path, x);
    }
  }

  @Override public void writeSuccess() {
  }

  private void handleClose(WebSocketCloseStatus closeStatus) {
    WebSocket.OnClose callback = this.onCloseCallback.getAndSet(null);
    Throwable cause = null;
    // 1. close socket
    try {
      if (isOpen()) {
        open.set(false);
        session.close(closeStatus.getCode(), closeStatus.getReason());
      }
    } catch (Throwable x) {
      cause = x;
    }
    // fire callback:
    if (callback != null) {
      try {
        callback.onClose(this, closeStatus);
      } catch (Throwable x) {
        if (cause != null) {
          x.addSuppressed(cause);
        }
        cause = x;
      }
    }
    // clear from active sessions:
    removeSession(this);

    if (cause != null) {
      // fire error:
      onWebSocketError(cause);
    }
  }

  private static void addSession(JettyWebSocket ws) {
    all.computeIfAbsent(ws.key, k -> new CopyOnWriteArrayList<>()).add(ws);
  }

  private static void removeSession(JettyWebSocket ws) {
    List<WebSocket> sockets = all.get(ws.key);
    if (sockets != null) {
      sockets.remove(ws);
    }
  }
}
