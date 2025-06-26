/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.nio.channels.ReadPendingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.StaticException;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.exceptions.CloseException;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketConfigurer;
import io.jooby.WebSocketMessage;
import io.jooby.buffer.DataBuffer;
import io.jooby.output.Output;

public class JettyWebSocket implements Session.Listener, WebSocketConfigurer, WebSocket {

  private static class WriteCallbackAdaptor implements org.eclipse.jetty.websocket.api.Callback {

    private JettyWebSocket ws;
    private WebSocket.WriteCallback callback;

    public WriteCallbackAdaptor(JettyWebSocket ws, WebSocket.WriteCallback callback) {
      this.ws = ws;
      this.callback = callback;
    }

    @Override
    public void fail(Throwable cause) {
      try {
        if (Server.connectionLost(cause)) {
          ws.ctx
              .getRouter()
              .getLog()
              .debug(
                  "WebSocket {} send method resulted in exception",
                  ws.getContext().getRequestPath(),
                  cause);
        } else {
          ws.ctx
              .getRouter()
              .getLog()
              .error(
                  "WebSocket {} send method resulted in exception",
                  ws.getContext().getRequestPath(),
                  cause);
        }
      } finally {
        callback.operationComplete(ws, cause);
      }
    }

    @Override
    public void succeed() {
      try {
        callback.operationComplete(ws, null);
      } finally {
        ws.demand();
      }
    }
  }

  /** All connected websocket. */
  private static final ConcurrentMap<String, List<JettyWebSocket>> all = new ConcurrentHashMap<>();

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

  @Override
  public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
    if (onMessageCallback != null) {
      try {
        onMessageCallback.onMessage(
            this, WebSocketMessage.create(getContext(), BufferUtil.toArray(payload)));
      } catch (Throwable x) {
        onWebSocketError(x);
      }
    }
  }

  @Override
  public void onWebSocketText(String message) {
    if (onMessageCallback != null) {
      try {
        onMessageCallback.onMessage(this, WebSocketMessage.create(getContext(), message));
      } catch (Throwable x) {
        onWebSocketError(x);
      }
    }
  }

  @Override
  public void onWebSocketClose(int statusCode, String reason) {
    handleClose(
        WebSocketCloseStatus.valueOf(statusCode)
            .orElseGet(() -> new WebSocketCloseStatus(statusCode, reason)));
  }

  @Override
  public void onWebSocketOpen(Session session) {
    try {
      open.set(true);
      this.session = session;
      addSession(this);
      demand();
      if (onConnectCallback != null) {
        onConnectCallback.onConnect(this);
      }
    } catch (Throwable x) {
      onWebSocketError(x);
    } finally {
      demand();
    }
  }

  @Override
  public void onWebSocketError(Throwable x) {
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
    return Server.connectionLost(x)
        || (x instanceof StaticException && x.getMessage().equals("Closed"));
  }

  private boolean isTimeout(Throwable x) {
    if (x instanceof CloseException) {
      Throwable cause = x.getCause();
      return cause instanceof TimeoutException;
    }
    return false;
  }

  @NonNull @Override
  public WebSocketConfigurer onConnect(@NonNull WebSocket.OnConnect callback) {
    onConnectCallback = callback;
    return this;
  }

  @NonNull @Override
  public WebSocketConfigurer onMessage(@NonNull WebSocket.OnMessage callback) {
    onMessageCallback = callback;
    return this;
  }

  @NonNull @Override
  public WebSocketConfigurer onError(@NonNull WebSocket.OnError callback) {
    onErrorCallback = callback;
    return this;
  }

  @NonNull @Override
  public WebSocketConfigurer onClose(@NonNull WebSocket.OnClose callback) {
    onCloseCallback.set(callback);
    return this;
  }

  @NonNull @Override
  public Context getContext() {
    return Context.readOnly(ctx);
  }

  private void demand() {
    try {
      session.demand();
    } catch (ReadPendingException cause) {
      ctx.getRouter().getLog().debug("Websocket resulted in exception: {}", path, cause);
    }
  }

  @NonNull @Override
  public List<WebSocket> getSessions() {
    List<JettyWebSocket> sessions = all.get(key);
    if (sessions == null) {
      return Collections.emptyList();
    }
    List<WebSocket> result = new ArrayList<>(sessions);
    result.remove(this);
    return result;
  }

  @Override
  public boolean isOpen() {
    return open.get() && session.isOpen();
  }

  @Override
  public void forEach(SneakyThrows.Consumer<WebSocket> callback) {
    for (JettyWebSocket ws : all.getOrDefault(key, Collections.emptyList())) {
      try {
        callback.accept(ws);
      } catch (Exception cause) {
        ctx.getRouter().getLog().debug("Broadcast of: {} resulted in exception", ws.path, cause);
      }
    }
  }

  @NonNull @Override
  public WebSocket sendBinary(@NonNull String message, @NonNull WriteCallback callback) {
    return sendMessage(
        (remote, writeCallback) ->
            remote.sendBinary(ByteBuffer.wrap(message.getBytes(UTF_8)), writeCallback),
        new WriteCallbackAdaptor(this, callback));
  }

  @NonNull @Override
  public WebSocket send(@NonNull String message, @NonNull WriteCallback callback) {
    return sendMessage(
        (remote, writeCallback) -> remote.sendText(message, writeCallback),
        new WriteCallbackAdaptor(this, callback));
  }

  @NonNull @Override
  public WebSocket send(@NonNull ByteBuffer message, @NonNull WriteCallback callback) {
    return sendMessage(
        (remote, writeCallback) -> remote.sendText(UTF_8.decode(message).toString(), writeCallback),
        new WriteCallbackAdaptor(this, callback));
  }

  @NonNull @Override
  public WebSocket send(@NonNull byte[] message, @NonNull WriteCallback callback) {
    return send(new String(message, UTF_8), callback);
  }

  @NonNull @Override
  public WebSocket sendBinary(@NonNull ByteBuffer message, @NonNull WriteCallback callback) {
    return sendMessage(
        (remote, writeCallback) -> remote.sendBinary(message, writeCallback),
        new WriteCallbackAdaptor(this, callback));
  }

  @NonNull @Override
  public WebSocket send(@NonNull DataBuffer message, @NonNull WriteCallback callback) {
    return this;
  }

  @NonNull @Override
  public WebSocket send(@NonNull Output message, @NonNull WriteCallback callback) {
    return sendMessage(
        (remote, writeCallback) -> remote.sendText(message.asString(UTF_8), writeCallback),
        new WriteCallbackAdaptor(this, callback));
  }

  @NonNull @Override
  public WebSocket sendBinary(@NonNull DataBuffer message, @NonNull WriteCallback callback) {
    return this;
  }

  @NonNull @Override
  public WebSocket sendBinary(@NonNull Output message, @NonNull WriteCallback callback) {
    return sendMessage(
        (remote, writeCallback) ->
            new WebSocketDataBufferCallback(writeCallback, message, remote::sendBinary).send(),
        new WriteCallbackAdaptor(this, callback));
  }

  private WebSocket sendMessage(BiConsumer<Session, Callback> writer, Callback callback) {
    if (isOpen()) {
      try {
        writer.accept(session, callback);
      } catch (Throwable x) {
        onWebSocketError(x);
      }
    } else {
      onWebSocketError(new IllegalStateException("Attempt to send a message on closed web socket"));
    }
    return this;
  }

  @NonNull @Override
  public WebSocket render(@NonNull Object value, @NonNull WriteCallback callback) {
    return renderMessage(value, false, callback);
  }

  @NonNull @Override
  public WebSocket renderBinary(@NonNull Object value, @NonNull WriteCallback callback) {
    return renderMessage(value, true, callback);
  }

  private WebSocket renderMessage(Object value, boolean binary, WriteCallback callback) {
    try {
      Context.websocket(ctx, this, binary, callback).render(value);
    } catch (Throwable x) {
      onWebSocketError(x);
    }
    return this;
  }

  @NonNull @Override
  public WebSocket close(@NonNull WebSocketCloseStatus closeStatus) {
    handleClose(closeStatus);
    return this;
  }

  private void handleClose(WebSocketCloseStatus closeStatus) {
    WebSocket.OnClose callback = this.onCloseCallback.getAndSet(null);
    Throwable cause = null;
    // 1. close socket
    try {
      if (isOpen()) {
        open.set(false);
        // TODO: jetty callback
        session.close(closeStatus.getCode(), closeStatus.getReason(), Callback.NOOP);
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
    List<JettyWebSocket> sockets = all.get(ws.key);
    if (sockets != null) {
      sockets.remove(ws);
    }
  }
}
