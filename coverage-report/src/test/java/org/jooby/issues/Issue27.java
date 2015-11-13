package org.jooby.issues;

import org.jooby.Results;
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

    get("/config", req -> Results.html("org/jooby/issues/27/config").put("this", new Object()));

    get("/req", req -> Results.html("org/jooby/issues/27/req").put("this", new Object()));

    get("/session", req -> {
      req.session().set("attr", "session-attr");
      return Results.html("org/jooby/issues/27/session").put("this", new Object());
    });
  }

  @Test
  public void shouldRenderConfigProperty() throws Exception {
    request()
        .get("/config")
        .expect("issues");
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
