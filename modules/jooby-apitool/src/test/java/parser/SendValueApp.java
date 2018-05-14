package parser;

import org.jooby.Jooby;

public class SendValueApp extends Jooby {

  {

    get("/api/path", (req, rsp) -> {
      String v = req.param("s").value();
      rsp.send(v);
    });

  }
}
