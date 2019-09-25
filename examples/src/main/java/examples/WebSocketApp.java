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
        ws.send("Got: " + msg.value(), true);
      });
      initializer.onClose((ws, closeStatus) -> {
        System.out.println("Closed " + closeStatus);
      });

      initializer.onError((ws, cause) -> {
        ws.getContext().getRouter().getLog().error("error ", cause);
      });

    });
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.DEFAULT, WebSocketApp::new);
  }
}
