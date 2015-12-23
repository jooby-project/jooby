package org.jooby.pebble;

import org.jooby.Results;
import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class PebbleLocalsFeature extends ServerFeature {

  {
    session(Session.Mem.class);

    use(new Pebble());

    get("*", (req, rsp) -> {
      req.session().set("a", "sessionA");
      req.set("app", req.require(Config.class).getConfig("application").root().unwrapped());
      req.set("attr", "x");
      req.set("session", req.session().attributes());
    });

    get("/", req -> {
      return Results.html("org/jooby/pebble/locals");
    });
  }

  @Test
  public void locals() throws Exception {
    request()
        .get("/")
        .expect("<html><body>dev:x:sessionA</body></html>");
  }
}
