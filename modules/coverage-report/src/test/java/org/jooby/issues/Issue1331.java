package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue1331 extends ServerFeature {

  {
    get("/1331", req -> {
      return req.cookie("login").value("not set");
    });
  }

  @Test
  public void shouldNotFailOnEmptyCookies() throws Exception {
    request()
        .get("/1331")
        .header("Cookie", "login=;Version=1;Path=/1331")
        .expect("");
    request()
        .get("/1331")
        .expect("not set");
  }

}
