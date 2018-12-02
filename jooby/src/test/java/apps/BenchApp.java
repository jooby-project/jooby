package apps;

import io.jooby.App;
import io.jooby.Filters;

public class BenchApp extends App {

  private static final String MESSAGE = "Hello World!";

  static class Message {
    public final String message;

    public Message(String message) {
      this.message = message;
    }
  }

  {
    filter(Filters.defaultHeaders());

    get("/", ctx -> ctx.sendText(MESSAGE));

    get("/json", ctx -> Thread.currentThread().getName());

    get("/fortune", ctx -> Thread.currentThread().getName());
  }

  public static void main(String[] args) {
//    new Utow()
//        .deploy(new BenchApp().mode(ExecutionMode.EVENT_LOOP))
//        .start()
//        .join();
  }
}
