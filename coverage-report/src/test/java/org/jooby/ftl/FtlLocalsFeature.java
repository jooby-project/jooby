package org.jooby.ftl;

import org.jooby.View;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class FtlLocalsFeature extends ServerFeature {

  {
    use(new Ftl());

    get("*", (req, rsp) -> {
      req.session().set("a", "A");
      req.set("app", req.require(Config.class).getConfig("application"));
      req.set("attr", "x");
      req.set("req", req);
      req.set("session", req.session());
    });

    get("/", req -> {
      return View.of("org/jooby/ftl/locals");
    });
  }

  @Test
  public void locals() throws Exception {
    request()
        .get("/")
        .expect("<html><body>dev:x:x:A</body></html>");
  }
}
