/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;

import java.nio.file.Paths;

public class WebSocketApp extends Jooby {
  {
    assets("/?*", Paths.get(System.getProperty("user.dir"), "examples", "www", "websocket"));

    ws("/ws", (ctx, initializer) -> {
      initializer.onConnect(ws -> {
        ws.send("Welcome");
      });
      initializer.onMessage((ws, msg) -> {
        ws.send("Got: " + msg.value());
      });
      initializer.onClose((ws, closeStatus) -> {
        getLog().info("Closing with: {}", closeStatus);
      });
    });
  }

  public static void main(String[] args) {
    runApp(args, WebSocketApp::new);
  }
}
