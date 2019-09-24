package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketApp extends Jooby {
  {
    assets("/?*", Paths.get(System.getProperty("user.dir"), "examples", "www", "websocket"));

    ScheduledExecutorService executor = Executors
        .newSingleThreadScheduledExecutor();
    ws("/ws", ctx -> {
      AtomicInteger counter = new AtomicInteger();
      ctx.onConnect(ws -> {
        executor.scheduleWithFixedDelay(() -> {
          ws.send("" + counter.incrementAndGet());
        }, 0, 3, TimeUnit.SECONDS);
      });
      ctx.onMessage((ws, msg) -> {
        System.out.println("msg: " + counter.incrementAndGet() + " => " + msg.value());
        System.out.println(Thread.currentThread());
        // ws.send("Got: " + msg.value());
      });
      ctx.onClose((ws, closeStatus) -> {
        System.out.println("Closed " + closeStatus);
      });

    });
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.DEFAULT, WebSocketApp::new);
    //    runApp(args, ExecutionMode.EVENT_LOOP, WebSocketApp::new);
  }
}
