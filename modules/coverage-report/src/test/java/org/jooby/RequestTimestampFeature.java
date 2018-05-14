package org.jooby;

import static org.junit.Assert.assertTrue;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestTimestampFeature extends ServerFeature {

  {
    get("/ts", req -> req.timestamp());
  }

  @Test
  public void timestamp() throws Exception {
    request()
        .get("/ts")
        .expect(v -> {
          assertTrue(Long.parseLong(v) > 0);
        });
  }
}
