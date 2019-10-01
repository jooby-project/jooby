/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Websocket client for unit tests.
 */
public class MockWebSocketClient {
  private MockWebSocketConfigurer configurer;
  private boolean open = true;
  private SneakyThrows.Consumer<Object> onMessageCallback;
  private SneakyThrows.Consumer2<Integer, String> onCloseCallback;
  private List<Runnable> actions = new ArrayList<>();
  private boolean initialized;

  MockWebSocketClient(MockWebSocketConfigurer configurer) {
    this.configurer = configurer;
  }

  /**
   * True for opened connection.
   *
   * @return True for opened connection.
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Send a websocket message to a websocket server.
   *
   * @param message Message.
   * @return This client.
   */
  public MockWebSocketClient send(@Nonnull Object message) {
    if (isOpen()) {
      configurer.fireOnMessage(message);
    } else {
      throw new IllegalStateException("Attempt to send a message on closed web socket");
    }
    return this;
  }

  /**
   * Close a websocket and send a close message to websocket server.
   *
   * @return This client.
   */
  public MockWebSocketClient close() {
    return close(WebSocketCloseStatus.NORMAL_CODE);
  }

  /**
   * Close a websocket and send a close message to websocket server.
   *
   * @param code Close status code.
   * @return This client.
   */
  public MockWebSocketClient close(int code) {
    return close(code, null);
  }

  /**
   * Close a websocket and send a close message to websocket server.
   *
   * @param code Close status code.
   * @param reason Close reason.
   * @return This client.
   */
  public MockWebSocketClient close(int code, @Nullable String reason) {
    if (isOpen()) {
      open = false;
      if (initialized) {
        configurer.fireClose(WebSocketCloseStatus.valueOf(code)
            .orElseGet(() -> new WebSocketCloseStatus(code, reason)));
      } else {
        actions.add(() -> close(code, reason));
      }
    }
    return this;
  }

  /**
   * Add an on message callback. Fire it when websocket server send a message.
   *
   * @param callback Callback to execute.
   * @return This client.
   */
  public MockWebSocketClient onMessage(SneakyThrows.Consumer<Object> callback) {
    onMessageCallback = callback;
    return this;
  }

  void fireMessage(Object message) {
    if (initialized) {
      if (onMessageCallback != null) {
        onMessageCallback.accept(message);
      }
    } else {
      actions.add(() -> fireMessage(message));
    }
  }

  void fireClose(WebSocketCloseStatus closeStatus) {
    open = false;
    if (initialized) {
      if (onCloseCallback != null) {
        onCloseCallback.accept(closeStatus.getCode(), closeStatus.getReason());
      }
    } else {
      actions.add(() -> fireClose(closeStatus));
    }
  }

  void init() {
    initialized = true;
    for (Runnable action : actions) {
      action.run();
    }
    actions.clear();
    actions = null;
  }
}
