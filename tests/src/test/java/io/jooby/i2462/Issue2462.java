/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2462;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import io.jooby.ServerOptions;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.test.WebClient;
import io.jooby.undertow.UndertowServer;

public class Issue2462 {
  @ServerTest(server = UndertowServer.class)
  public void shouldSendMultipleMessageWithoutError(ServerTestRunner runner) {
    char[] array = new char[ServerOptions._16KB + 1024];
    Arrays.fill(array, 'A');
    String message = new String(array);
    int messageCount = 100;
    runner
        .define(
            app -> {
              app.install(new JacksonModule());

              app.sse(
                  "/2462",
                  sse -> {
                    for (int i = 0; i < messageCount; i++) {
                      sse.send(message + i);
                    }
                  });
            })
        .ready(
            client -> {
              WebClient.ServerSentMessageIterator verifier = client.sse("/2462");
              for (int i = 0; i < messageCount; i++) {
                int index = i;
                verifier.next(
                    msg -> {
                      assertEquals(message + index, msg.getData());
                    });
              }
              verifier.verify();
            });
  }
}
