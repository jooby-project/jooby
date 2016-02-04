package apps;

import org.jooby.Jooby;

public class DefStr extends Jooby {

  {
    get("/", req -> {
      String value = req.param("value").value("value");
      return value;
    });
  }

}
