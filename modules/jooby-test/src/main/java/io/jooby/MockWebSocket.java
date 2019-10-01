/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * Mock implementation of {@link WebSocket} for unit testing purpose.
 *
 * App:
 * <pre>{@code
 *   ws("/path", (ctx, initializer) -> {
 *     initializer.onConnect(ws -> {
 *       ws.send("OnConnect");
 *     });
 *   });
 * }</pre>
 *
 * Test:
 * <pre>{@code
 *   MockRouter router = new MockRouter(new App());
 *   router.ws("/path", ws -> {
 *
 *     ws.onMessage(message -> {
 *       System.out.println("Got: " + message);
 *     });
 *
 *     ws.send("Another message");
 *   })
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

  @Nonnull @Override public Context getContext() {
    return ctx;
  }

  @Nonnull @Override public List<WebSocket> getSessions() {
    return Collections.emptyList();
  }

  @Override public boolean isOpen() {
    return open;
  }

  @Nonnull @Override public WebSocket send(@Nonnull String message, boolean broadcast) {
    return sendObject(message, broadcast);
  }

  @Nonnull @Override public WebSocket send(@Nonnull byte[] message, boolean broadcast) {
    return sendObject(message, broadcast);
  }

  @Nonnull @Override public WebSocket render(@Nonnull Object value, boolean broadcast) {
    return sendObject(value, broadcast);
  }

  @Nonnull @Override public WebSocket close(@Nonnull WebSocketCloseStatus closeStatus) {
    try {
      open = false;
      configurer.fireClose(closeStatus);
    } catch (Throwable x) {
      handleError(x);
    }
    return this;
  }

  private WebSocket sendObject(Object message, boolean broadcast) {
    try {
      if (open) {
        configurer.fireClientMessage(message);
      } else {
        throw new IllegalStateException("Attempt to send a message on closed web socket");
      }
    } catch (Throwable x) {
      handleError(x);
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
