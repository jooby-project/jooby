package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jooby.Status;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ErrHandlerFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> {
      throw new IllegalArgumentException();
    });

    err((req, rsp, ex) -> {
      assertTrue(ex instanceof IllegalArgumentException);
      assertEquals(Status.BAD_REQUEST, rsp.status().get());
      rsp.send("err...");
    });
  }

  @Test
  public void err() throws Exception {
    request()
        .get("/")
        .expect("err...")
        .expect(400);
  }

}
