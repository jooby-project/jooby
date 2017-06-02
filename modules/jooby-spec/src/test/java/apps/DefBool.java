package apps;

import org.jooby.Jooby;

public class DefBool extends Jooby {

  {
    get("/", req -> {
      boolean t = req.param("t").booleanValue(true);

      boolean f = req.param("f").booleanValue(false);

      boolean r = t && f;
      return r;
    });
  }

}
