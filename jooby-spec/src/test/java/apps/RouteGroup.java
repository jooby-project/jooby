package apps;

import org.jooby.Jooby;

public class RouteGroup extends Jooby {

  {
    use("/group").get("/", () -> {
      return 0;
    });
  }

}
