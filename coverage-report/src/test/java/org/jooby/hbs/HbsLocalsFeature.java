package org.jooby.hbs;

import org.jooby.View;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class HbsLocalsFeature extends ServerFeature {

  {
    use(new Hbs());

    get("*", (req, rsp) -> {
      req.set("app", req.require(Config.class).getConfig("application"));
      req.set("attr", "x");
      req.set("req", req);
    });

    get("/", req -> {
      return View.of("org/jooby/hbs/locals");
    });
  }

  @Test
  public void locals() throws Exception {
    request()
        .get("/")
        .expect("<html><body>dev:x:x</body></html>");
  }
}
