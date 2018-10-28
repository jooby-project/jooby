package apps;

import io.jooby.App;
import io.jooby.Filters;
import io.jooby.Mode;
import io.jooby.utow.Utow;

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
  }

  public static void main(String[] args) {
    new Utow()
        .deploy(new BenchApp().mode(Mode.IO))
        .start()
        .join();
  }
}
