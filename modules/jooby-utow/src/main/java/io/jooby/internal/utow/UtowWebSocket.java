/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import static io.undertow.websockets.core.WebSockets.sendClose;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import java.util.concurrent.atomic.AtomicReference;

import org.xnio.IoUtils;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
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

public class UtowWebSocket extends AbstractReceiveListener
    implements WebSocketConfigurer, WebSocket, WebSocketCallback<Void> {

  /** All connected websocket. */
  private static final ConcurrentMap<String, List<WebSocket>> all = new ConcurrentHashMap<>();

  private final UtowContext ctx;
  private final WebSocketChannel channel;
  private final boolean dispatch;
  private OnConnect onConnectCallback;
  private OnMessage onMessageCallback;
  private AtomicReference<OnClose> onCloseCallback = new AtomicReference<>();
  private OnError onErrorCallback;
  private String key;
  private CountDownLatch ready = new CountDownLatch(1);
  private AtomicBoolean open = new AtomicBoolean(false);
  private int maxSize;

  public UtowWebSocket(UtowContext ctx, WebSocketChannel channel) {
    this.ctx = ctx;
    this.channel = channel;
    this.dispatch = !ctx.isInIoThread();
    this.key = ctx.getRoute().getPattern();

    Config conf = ctx.getRouter().getConfig();
    maxSize =
        conf.hasPath("websocket.maxSize")
            ? conf.getBytes("websocket.maxSize").intValue()
            : WebSocket.MAX_BUFFER_SIZE;
  }

  @Override
  protected long getMaxTextBufferSize() {
    return maxSize;
  }

  @Override
  protected long getMaxBinaryBufferSize() {
    return maxSize;
  }

  @NonNull @Override
  public Context getContext() {
    return Context.readOnly(ctx);
  }

  @NonNull @Override
  public List<WebSocket> getSessions() {
    List<WebSocket> sessions = all.get(key);
    if (sessions == null) {
      return Collections.emptyList();
    }
    List<WebSocket> result = new ArrayList<>(sessions);
    result.remove(this);
    return result;
  }

  @Override
  public boolean isOpen() {
    return open.get() && channel.isOpen();
  }

  @NonNull @Override
  public WebSocket send(@NonNull String message, boolean broadcast) {
    return send(message.getBytes(StandardCharsets.UTF_8), broadcast);
  }

  @NonNull @Override
  public WebSocket send(@NonNull byte[] message, boolean broadcast) {
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
        onError(
            channel, new IllegalStateException("Attempt to send a message on closed web socket"));
      }
    }
    return this;
  }

  @NonNull @Override
  public WebSocket render(@NonNull Object value, boolean broadcast) {
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

  @NonNull @Override
  public WebSocket close(@NonNull WebSocketCloseStatus closeStatus) {
    handleClose(closeStatus);
    return this;
  }

  @NonNull @Override
  public WebSocketConfigurer onConnect(@NonNull OnConnect callback) {
    onConnectCallback = callback;
    return this;
  }

  @NonNull @Override
  public WebSocketConfigurer onMessage(@NonNull OnMessage callback) {
    onMessageCallback = callback;
    return this;
  }

  @NonNull @Override
  public WebSocketConfigurer onError(@NonNull OnError callback) {
    onErrorCallback = callback;
    return this;
  }

  @NonNull @Override
  public WebSocketConfigurer onClose(@NonNull OnClose callback) {
    onCloseCallback.set(callback);
    return this;
  }

  void fireConnect() {
    // fire only once
    try {
      open.set(true);
      addSession(this);
      Config conf = ctx.getRouter().getConfig();
      long timeout =
          conf.hasPath("websocket.idleTimeout")
              ? conf.getDuration("websocket.idleTimeout", TimeUnit.MILLISECONDS)
              : TimeUnit.MINUTES.toMillis(5);
      if (timeout > 0) {
        channel.setIdleTimeout(timeout);
      }
      if (onConnectCallback != null) {
        dispatch(webSocketTask(() -> onConnectCallback.onConnect(this), true));
      } else {
        ready.countDown();
      }
      channel.getReceiveSetter().set(this);
      channel.resumeReceives();
    } catch (Throwable x) {
      onError(channel, x);
    }
  }

  @Override
  protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message)
      throws IOException {
    waitForConnect();

    if (onMessageCallback != null) {
      dispatch(
          webSocketTask(
              () ->
                  onMessageCallback.onMessage(
                      this, WebSocketMessage.create(getContext(), message.getData())),
              false));
    }
  }

  private void waitForConnect() {
    try {
      ready.await();
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }
  }

  private void dispatch(Runnable runnable) {
    if (dispatch) {
      ctx.getRouter().getWorker().execute(runnable);
    } else {
      runnable.run();
    }
  }

  @Override
  protected void onError(WebSocketChannel channel, Throwable x) {
    // should close?
    if (Server.connectionLost(x) || SneakyThrows.isFatal(x)) {
      if (isOpen()) {
        handleClose(WebSocketCloseStatus.SERVER_ERROR);
      }
    }

    if (onErrorCallback == null) {
      if (Server.connectionLost(x)) {
        ctx.getRouter().getLog().debug("Websocket connection lost: {}", ctx.getRequestPath(), x);
      } else {
        ctx.getRouter()
            .getLog()
            .error("Websocket resulted in exception: {}", ctx.getRequestPath(), x);
      }
    } else {
      onErrorCallback.onError(this, x);
    }

    if (SneakyThrows.isFatal(x)) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
    if (isOpen()) {
      handleClose(
          WebSocketCloseStatus.valueOf(cm.getCode())
              .orElseGet(() -> new WebSocketCloseStatus(cm.getCode(), cm.getReason())));
    }
  }

  private void handleClose(WebSocketCloseStatus status) {
    OnClose callback = onCloseCallback.getAndSet(null);
    if (isOpen()) {
      open.set(false);
      // close socket:
      sendClose(
          status.getCode(),
          status.getReason(),
          channel,
          new WebSocketCallback<UtowWebSocket>() {
            @Override
            public void onError(
                final WebSocketChannel channel, final UtowWebSocket ws, final Throwable throwable) {
              IoUtils.safeClose(channel);
              ws.onError(channel, throwable);
            }

            @Override
            public void complete(final WebSocketChannel channel, final UtowWebSocket ws) {
              IoUtils.safeClose(channel);
            }
          },
          this);
    }
    try {
      // fire callback:
      if (callback != null) {
        callback.onClose(this, status);
      }
    } catch (Throwable x) {
      // fire error:
      onError(channel, x);
    } finally {
      // clear from active sessions:
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

  @Override
  public void complete(WebSocketChannel channel, Void context) {
    // NOOP
  }

  @Override
  public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
    ctx.getRouter().getLog().error("WebSocket.send resulted in exception", throwable);
  }

  private Runnable webSocketTask(Runnable runnable, boolean isInit) {
    return () -> {
      try {
        runnable.run();
      } catch (Throwable x) {
        onError(null, x);
      } finally {
        if (isInit) {
          ready.countDown();
        }
      }
    };
  }
}
