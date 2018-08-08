package apps;

import io.jooby.App;
import io.jooby.Mode;
import io.jooby.jackson.Jackson;
import io.jooby.jetty.Jetty;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;

import java.util.Collections;

public class HelloApp extends App {

  static class Message {
    public final String message;

    public Message(String message) {
      this.message = message;
    }
  }

  {
    get("/", ctx -> "Hello World!");

    get("/{foo}", ctx -> "Hello World!");

    dispatch(() -> {
      filter(next -> ctx -> {
        System.out.println(Thread.currentThread());
        return next.apply(ctx);
      });
      get("/worker", ctx -> "Hello Worker");
    });

    renderer(new Jackson());
    get("/json", ctx -> new Message("Hello World!"));

    error((ctx, cause, statusCode) -> {
      ctx.statusCode(statusCode)
          .send(statusCode.reason());
    });
  }

  public static void main(String[] args) {
    HelloApp app = new HelloApp();
    app.mode(Mode.IO);
    app.use(new Utow());
    app.start();
  }
}
