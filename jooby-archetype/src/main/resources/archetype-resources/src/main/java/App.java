package ${package};

import org.jooby.Jooby;

/**
 * @author jooby generator
 */
public class App extends Jooby {

  {
    get("/", () -> "Hello World!");
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }

}
