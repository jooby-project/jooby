package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.time.ZonedDateTime;

public class Issue1073 extends ServerFeature {

  {
    get("/1073", req -> {
      ZonedDateTime time = req.param("start").to(ZonedDateTime.class);
      return time;
    });

    post("/1073", req -> {
      ZonedDateTime time = req.body(ZonedDateTime.class);
      return time;
    });
  }

  @Test
  public void shouldParseZonedDateTime() throws Exception {
    request().get("/1073?start=2017-01-27T15:12:07.6%2B01:00")
        .expect("2017-01-27T15:12:07.600+01:00");
    request().post("/1073")
        .body("2017-01-27T15:12:07.6Z", "text/plain")
        .expect("2017-01-27T15:12:07.600Z");
  }
}
