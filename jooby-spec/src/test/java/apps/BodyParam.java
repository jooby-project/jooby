package apps;

import org.jooby.Jooby;

public class BodyParam extends Jooby {

  {
    post("/create", req -> {
      LocalType result = req.body().to(LocalType.class);
      return result;
    });
  }

}
