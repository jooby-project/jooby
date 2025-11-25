/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3813;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class Issue3813 {

  @ServerTest
  public void shouldSendPingMessage(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.ws(
                  "/3813",
                  (ctx, initializer) -> {
                    initializer.onConnect(
                        ws -> {
                          ws.sendPing("Hello, World!");
                        });
                  });
            })
        .ready(
            client -> {
              client.webSocket(
                  "/3813",
                  new WebSocketListener() {
                    @Override
                    public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {}

                    @Override
                    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                      super.onMessage(webSocket, text);
                    }

                    @Override
                    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                      super.onMessage(webSocket, bytes);
                    }

                    @Override
                    public void onClosing(
                        @NonNull WebSocket webSocket, int code, @NonNull String reason) {
                      super.onClosing(webSocket, code, reason);
                    }

                    @Override
                    public void onClosed(
                        @NonNull WebSocket webSocket, int code, @NonNull String reason) {
                      super.onClosed(webSocket, code, reason);
                    }

                    @Override
                    public void onFailure(
                        @NonNull WebSocket webSocket,
                        @NonNull Throwable t,
                        @Nullable Response response) {
                      super.onFailure(webSocket, t, response);
                    }
                  });
            });
  }
}
