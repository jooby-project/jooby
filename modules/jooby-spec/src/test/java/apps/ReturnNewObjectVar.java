package apps;

import org.jooby.Jooby;

public class ReturnNewObjectVar extends Jooby {

  {
    get("/", req -> {
      LocalType var = new LocalType();
      return var;
    });
  }

}
