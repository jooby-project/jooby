package examples;

import io.jooby.Jooby;

public class RouteImport extends Jooby {

  {
    use(new RouteA());

    path("/main", () -> {
      use(new RouteA());

      use("/submain", new RouteA());
    });

    use(new RouteA());

    use("/require", require(RouteA.class));

    use("/subroute", new RouteA());
  }
}
