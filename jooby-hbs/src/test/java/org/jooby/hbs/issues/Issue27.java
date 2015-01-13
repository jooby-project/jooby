package org.jooby.hbs.issues;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
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

    get("/config", req -> View.of("config", new Object()));

    get("/req", req -> View.of("req", new Object()));

    get("/session", req -> {
      req.session().set("attr", "session-attr");
      return View.of("session", new Object());
    });
  }

  @Test
  public void shouldRenderConfigProperty() throws Exception {
    assertEquals("Issue27", Request.Get(uri("/config").build()).execute().returnContent()
        .asString());
  }

  @Test
  public void shouldRenderRequestProperty() throws Exception {
    assertEquals("/req", Request.Get(uri("/req").build()).execute().returnContent().asString());
  }

  @Test
  public void shouldRenderSessionProperty() throws Exception {
    assertEquals("session-attr", Request.Get(uri("/session").build()).execute().returnContent()
        .asString());
  }

}
