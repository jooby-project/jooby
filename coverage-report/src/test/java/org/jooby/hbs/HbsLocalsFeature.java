package org.jooby.hbs;

import org.jooby.Results;
import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class HbsLocalsFeature extends ServerFeature {

  {
    session(Session.Mem.class);

    use(new Hbs());

    get("*", (req, rsp) -> {
      req.session().set("a", "A");
      req.set("app", req.require(Config.class).getConfig("application"));
      req.set("attr", "x");
      req.set("req", req);
      req.set("session", req.session());
    });

    get("/", req -> {
      return Results.html("org/jooby/hbs/locals");
    });
  }

  @Test
  public void locals() throws Exception {
    request()
        .get("/")
        .expect("<html><body>dev:x:x:A</body></html>");
  }
}
