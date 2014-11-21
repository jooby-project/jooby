package ${package};

import org.jooby.Jooby;

/**
 * @author jooby generator
 */
public class App extends Jooby {

  {
    get("/favicon.ico");

    assets("/assets/**");

    get("/", staticFile("welcome.html"));
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }

}
