package apps;

import io.jooby.App;
import io.jooby.DefaultHeaders;
import io.jooby.MediaType;
import io.jooby.Mode;
import io.jooby.jackson.Jackson;
import io.jooby.jetty.Jetty;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;

public class HelloApp extends App {

  private static final String MESSAGE = "Hello World!";

  static class Message {
    public final String message;

    public Message(String message) {
      this.message = message;
    }
  }

  {
    defaultContentType("text/plain");

    filter(new DefaultHeaders());

    get("/", ctx -> ctx.sendText(MESSAGE));

    get("/{foo}", ctx -> ctx.sendText("Hello World!"));

    dispatch(() -> {
      filter(next -> ctx -> {
        System.out.println(Thread.currentThread());
        return next.apply(ctx);
      });
      get("/worker", ctx -> ctx.sendText("Hello World!"));
    });

    renderer(new Jackson());
    get("/json", ctx -> ctx.type("application/json").send(new Message("Hello World!")));

    error((ctx, cause, statusCode) -> {
      ctx.statusCode(statusCode)
          .sendText(statusCode.reason());
    });
  }

  public static void main(String[] args) {
    HelloApp app = new HelloApp();
    app.mode(Mode.IO);
    app.use(new Utow());
    app.start();
  }
}
