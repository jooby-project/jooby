package parser;

import org.jooby.Jooby;

public class UseRouteApp extends Jooby {

  {

    use("/api/path", req -> {
      String v = req.param("wild").value();
      return v;
    });

  }
}
