package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketApp extends Jooby {
  {
    assets("/", Paths.get(System.getProperty("user.dir"), "examples", "www", "websocket"));

    ws("/ws", ctx -> {
      AtomicInteger counter = new AtomicInteger();
//      ws.onConnect(ctx -> {
//        System.out.println("connect: " + counter.incrementAndGet());
//      });
//      ws.onMessage((ctx, msg) -> {
//        System.out.println("msg: " + counter.incrementAndGet() + " => " + msg);
//        System.out.println(Thread.currentThread());
//      });
    });
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.DEFAULT, WebSocketApp::new);
//    runApp(args, ExecutionMode.EVENT_LOOP, WebSocketApp::new);
  }
}
