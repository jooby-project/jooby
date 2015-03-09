package org.jooby.issues;

import org.jooby.View;
import org.jooby.hbs.Hbs;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class Issue27 extends ServerFeature {

  {
    use(new Hbs());

    get("*", (req, rsp) -> req.set("config", req.require(Config.class)));

    get("*", (req, rsp) -> req.set("req", req));

    get("*", (req, rsp) -> req.set("session", req.session()));

    get("/config", req -> View.of("org/jooby/issues/27/config", new Object()));

    get("/req", req -> View.of("org/jooby/issues/27/req", new Object()));

    get("/session", req -> {
      req.session().set("attr", "session-attr");
      return View.of("org/jooby/issues/27/session", new Object());
    });
  }

  @Test
  public void shouldRenderConfigProperty() throws Exception {
    request()
        .get("/config")
        .expect("Issue27");
  }

  @Test
  public void shouldRenderRequestProperty() throws Exception {
    request()
        .get("/req")
        .expect("/req");
  }

  @Test
  public void shouldRenderSessionProperty() throws Exception {
    request()
        .get("/session")
        .expect("session-attr");
  }

}
