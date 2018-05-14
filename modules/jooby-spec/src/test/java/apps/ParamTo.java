package apps;

import org.jooby.Jooby;

public class ParamTo extends Jooby {

  {
    get("/SingleGetParam", req -> {
      req.param("i").to(Integer.class);
      req.param("s").to(String.class);
      req.param("l").to(LocalType.class);
      req.param("q").to(java.util.Calendar.class);
      req.file("u");
      return "x";
    });
  }

}
