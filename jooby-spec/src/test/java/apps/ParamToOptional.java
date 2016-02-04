package apps;

import org.jooby.Jooby;

public class ParamToOptional extends Jooby {

  {
    get("/SingleGetOptionalParam", req -> {
      req.param("i").toOptional(Integer.class);
      req.param("s").toOptional();
      req.param("l").toOptional(LocalType.class);
      return "x";
    });
  }

}
