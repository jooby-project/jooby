/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3814;

import java.util.concurrent.CountDownLatch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class Issue3814 {

  @ServerTest
  public void shouldJettyWebSocketWorks(ServerTestRunner runner) {
    int messageCount = 10;
    var latch = new CountDownLatch(messageCount);
    runner
        .define(
            app -> {
              app.ws(
                  "/3814",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) -> {
                          latch.countDown();
                        });
                  });
            })
        .ready(
            client -> {
              client.webSocket(
                  "/3814",
                  new WebSocketListener() {
                    @Override
                    public void onOpen(@NotNull WebSocket ws, @NotNull Response response) {
                      for (int i = 0; i < messageCount; i++) {
                        ws.send(">" + i);
                      }
                    }

                    @Override
                    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                      super.onMessage(webSocket, text);
                    }

                    @Override
                    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                      super.onMessage(webSocket, bytes);
                    }

                    @Override
                    public void onClosing(
                        @NotNull WebSocket webSocket, int code, @NotNull String reason) {
                      super.onClosing(webSocket, code, reason);
                    }

                    @Override
                    public void onClosed(
                        @NotNull WebSocket webSocket, int code, @NotNull String reason) {
                      super.onClosed(webSocket, code, reason);
                    }

                    @Override
                    public void onFailure(
                        @NotNull WebSocket webSocket,
                        @NotNull Throwable t,
                        @Nullable Response response) {
                      super.onFailure(webSocket, t, response);
                    }
                  });
              latch.await();
            });
  }
}
