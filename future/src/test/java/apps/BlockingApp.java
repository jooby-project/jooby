package apps;

import io.jooby.App;
import io.jooby.Mode;
import io.jooby.jetty.Jetty;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class BlockingApp extends App {
  {
    get("/", ctx -> {
      System.out.println("Scheduled: " + Thread.currentThread().getName());
      return Thread.currentThread().getName();
    });
  }

  public static void main(String[] args) {
    new Jetty()
        .deploy(new BlockingApp().mode(Mode.NIO))
        .start()
        .join();
  }
}
