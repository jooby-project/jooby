package apps;

import io.jooby.App;

public class BlockingApp extends App {
  {
    get("/", ctx -> {
      System.out.println("Scheduled: " + Thread.currentThread().getName());
      return Thread.currentThread().getName();
    });
  }

  public static void main(String[] args) {
//    new Netty()
//        .deploy(new BlockingApp().mode(ExecutionMode.EVENT_LOOP))
//        .start()
//        .join();
  }
}
