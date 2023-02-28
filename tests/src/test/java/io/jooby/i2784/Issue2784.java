/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2784;

import java.util.concurrent.CountDownLatch;

import org.asynchttpclient.Dsl;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2784 {

  @ServerTest
  public void shouldImplementWebSocketPingPong(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.ws(
                  "/2784",
                  (ctx, init) -> {
                    init.onMessage(
                        (ws, message) -> {
                          ws.send("back: " + message.value());
                        });
                  });
            })
        .ready(
            http -> {
              CountDownLatch latch = new CountDownLatch(1);
              WebSocketUpgradeHandler.Builder upgradeHandlerBuilder =
                  new WebSocketUpgradeHandler.Builder();
              WebSocketUpgradeHandler wsHandler =
                  upgradeHandlerBuilder
                      .addWebSocketListener(
                          new WebSocketListener() {
                            @Override
                            public void onTextFrame(
                                String payload, boolean finalFragment, int rsv) {}

                            @Override
                            public void onOpen(WebSocket websocket) {}

                            @Override
                            public void onClose(WebSocket websocket, int code, String reason) {}

                            @Override
                            public void onError(Throwable t) {}

                            @Override
                            public void onPongFrame(byte[] payload) {
                              latch.countDown();
                            }
                          })
                      .build();

              WebSocket webSocketClient =
                  Dsl.asyncHttpClient()
                      .prepareGet("ws://localhost:" + http.getPort() + "/2784")
                      .setRequestTimeout(5000)
                      .execute(wsHandler)
                      .get();
              // Send ping
              webSocketClient.sendPingFrame();

              latch.await();
            });
  }
}
