package org.jooby;

import java.nio.charset.StandardCharsets;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ByteBodyFeature extends ServerFeature {

  {
    post("/bytes", req -> {
      return new String(req.body(byte[].class), StandardCharsets.UTF_8);
    });
  }

  @Test
  public void shouldReadBodyAsByteArray() throws Exception {
    request()
        .post("/bytes")
        .body("foo", "text/plain")
        .expect("foo");
  }
}
