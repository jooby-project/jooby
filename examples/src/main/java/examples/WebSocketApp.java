/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.WebSocketCloseStatus;

import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketApp extends Jooby {
  {
    assets("/?*", Paths.get(System.getProperty("user.dir"), "examples", "www", "websocket"));

    ws("/ws", (ctx, initializer) -> {
      System.out.println(Thread.currentThread());
      System.out.println("Response Started: " + ctx.isResponseStarted());
      initializer.onConnect(ws -> {
        System.out.println("Connected: " + Thread.currentThread());
        ws.send("Welcome");
      });
      initializer.onMessage((ws, msg) -> {
        throw new UnsupportedOperationException("OnMessage");
//        ws.send("Got: " + msg.value());
      });
      initializer.onClose((ws, closeStatus) -> {
        System.out.println("Closed " + closeStatus);
      });

      initializer.onError((ws, cause) -> {
        if (ws.isOpen()) {
          ws.send("error: " + cause.getMessage() );
        } else {
          ws.getContext().getRouter().getLog().error("error ", cause);
        }
      });

    });
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.DEFAULT, WebSocketApp::new);
  }
}
