/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Allow to configure a websocket client for unit tests.
 *
 * @author edgar
 * @since 2.2.0
 */
public class MockWebSocketConfigurer implements WebSocketConfigurer {
  private final Context ctx;
  private final WebSocket.Initializer initializer;
  private final MockWebSocketClient client;
  private final MockWebSocket ws;
  private WebSocket.OnClose onClose;
  private WebSocket.OnConnect onConnect;
  private WebSocket.OnMessage onMessage;
  private WebSocket.OnError onError;

  MockWebSocketConfigurer(Context ctx, WebSocket.Initializer initializer) {
    this.ctx = ctx;
    this.initializer = initializer;
    this.initializer.init(ctx, this);
    this.client = new MockWebSocketClient(this);
    this.ws = new MockWebSocket(ctx, this);
  }

  @Nonnull @Override public WebSocketConfigurer onConnect(@Nonnull WebSocket.OnConnect callback) {
    this.onConnect = callback;
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onMessage(@Nonnull WebSocket.OnMessage callback) {
    this.onMessage = callback;
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onError(@Nonnull WebSocket.OnError callback) {
    this.onError = callback;
    return this;
  }

  @Nonnull @Override public WebSocketConfigurer onClose(@Nonnull WebSocket.OnClose callback) {
    this.onClose = callback;
    return this;
  }

  MockWebSocketClient getClient() {
    return client;
  }

  void fireClientMessage(Object message) {
    client.fireMessage(message);
  }

  void ready() {
    try {
      if (onConnect != null) {
        onConnect.onConnect(ws);
      }
    } catch (Throwable x) {
      fireError(x);
    }
  }

  void fireOnMessage(Object message) {
    if (onMessage != null) {
      onMessage.onMessage(ws, message instanceof WebSocketMessage
          ? (WebSocketMessage) message
          : WebSocketMessage.create(ctx, message.toString()));
    }
  }

  void fireClose(WebSocketCloseStatus closeStatus) {
    if (onClose != null) {
      onClose.onClose(ws, closeStatus);
    }
    client.fireClose(closeStatus);
  }

  void fireError(Throwable cause) {
    if (onError != null) {
      onError.onError(ws, cause);
    } else {
      LoggerFactory.getLogger(MockWebSocket.class).error("WebSocket resulted in exception", cause);
    }
  }
}
