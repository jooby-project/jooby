package apps;

import org.jooby.Jooby;

public class DefDouble extends Jooby {

  {
    get("/", req -> {
      double value = req.param("d").doubleValue(3.7);
      return value;
    });
  }

}
