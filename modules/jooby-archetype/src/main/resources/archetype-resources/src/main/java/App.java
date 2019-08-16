package ${package};

import io.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", ctx -> "Welcome to Jooby!");
  }

  public static void main(final String[] args) {
    runApp(args, App::new);
  }

}
