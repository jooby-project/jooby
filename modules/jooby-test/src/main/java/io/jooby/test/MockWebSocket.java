/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.output.Output;

/**
 * Mock implementation of {@link WebSocket} for unit testing purpose.
 *
 * <p>App:
 *
 * <pre>{@code
 * ws("/path", (ctx, initializer) -> {
 *   initializer.onConnect(ws -> {
 *     ws.send("OnConnect");
 *   });
 * });
 * }</pre>
 *
 * Test:
 *
 * <pre>{@code
 * MockRouter router = new MockRouter(new App());
 * router.ws("/path", ws -> {
 *
 *   ws.onMessage(message -> {
 *     System.out.println("Got: " + message);
 *   });
 *
 *   ws.send("Another message");
 * })
 * }</pre>
 *
 * @author edgar
 * @since 2.2.0
 */
public class MockWebSocket implements WebSocket {
  private final MockWebSocketConfigurer configurer;
  private Context ctx;
  private boolean open = true;

  MockWebSocket(Context ctx, MockWebSocketConfigurer configurer) {
    this.ctx = ctx;
    this.configurer = configurer;
  }

  @Override
  public Context getContext() {
    return ctx;
  }

  @Override
  public List<WebSocket> getSessions() {
    return Collections.emptyList();
  }

  @Override
  public boolean isOpen() {
    return open;
  }

  @Override
  public void forEach(SneakyThrows.Consumer<WebSocket> callback) {
    callback.accept(this);
  }

  @Override
  public WebSocket sendPing(String message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket sendPing(ByteBuffer message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket send(String message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket send(byte[] message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket send(ByteBuffer message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket send(Output message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket sendBinary(String message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket sendBinary(byte[] message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket sendBinary(ByteBuffer message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket sendBinary(Output message, WriteCallback callback) {
    return sendObject(message, callback);
  }

  @Override
  public WebSocket render(Object value, WriteCallback callback) {
    return sendObject(value, callback);
  }

  @Override
  public WebSocket renderBinary(Object value, WriteCallback callback) {
    return sendObject(value, callback);
  }

  @Override
  public WebSocket close(WebSocketCloseStatus closeStatus) {
    try {
      open = false;
      configurer.fireClose(closeStatus);
    } catch (Throwable x) {
      handleError(x);
    }
    return this;
  }

  private WebSocket sendObject(Object message, WriteCallback callback) {
    try {
      if (open) {
        configurer.fireClientMessage(message);
        if (callback != null) {
          callback.operationComplete(this, null);
        }
      } else {
        throw new IllegalStateException("Attempt to send a message on closed web socket");
      }
    } catch (Throwable x) {
      handleError(x);
      if (callback != null) {
        callback.operationComplete(this, x);
      }
    }
    return this;
  }

  private void handleError(Throwable error) {
    configurer.fireError(error);

    if (SneakyThrows.isFatal(error)) {
      throw SneakyThrows.propagate(error);
    }
  }
}
