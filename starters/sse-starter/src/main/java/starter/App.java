package starter;

import io.jooby.Jooby;

import java.nio.file.Paths;

public class App extends Jooby {
  {
    assets("/", Paths.get(System.getProperty("user.dir"), "views", "index.html"));

    sse("/sse", sse -> {
      sse.send("ping");
      sse.send("another");
    });
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
