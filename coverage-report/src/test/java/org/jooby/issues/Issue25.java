package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class Issue25 extends ServerFeature {

  {
    get("/contextPath", req -> {
      assertEquals("", req.ifGet("contextPath").get());
      assertEquals("/", req.require(Config.class).getString("application.path"));
      return req.path();
    });

    get("/req-path", req -> {
      assertEquals("/req-path", req.ifGet("path").get());
      assertEquals("/req-path", req.path());
      return req.path();
    });
  }

  @Test
  public void shouldSetApplicationPath() throws Exception {
    request()
        .get("/contextPath")
        .expect("/contextPath")
        .expect(200);
  }

  @Test
  public void shouldSetRequestPath() throws Exception {
    request()
        .get("/req-path")
        .expect("/req-path")
        .expect(200);
  }

}
