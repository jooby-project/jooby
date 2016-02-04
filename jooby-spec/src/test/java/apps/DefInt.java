package apps;

import org.jooby.Jooby;

public class DefInt extends Jooby {

  {
    get("/", req -> {
      int start = req.param("start").intValue(0);
      req.param("max").intValue(200);
      return start;
    });
  }

}
