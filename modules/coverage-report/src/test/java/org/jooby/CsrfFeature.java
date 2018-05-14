package org.jooby;

import static org.junit.Assert.assertEquals;

import org.jooby.handlers.CsrfHandler;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class CsrfFeature extends ServerFeature {

  {
    use("*", new CsrfHandler());

    get("/csrf", req -> req.ifGet("csrf").get());

    post("/csrf", req -> req.ifGet("csrf").get());
  }

  @Test
  public void csrf() throws Exception {
    request()
        .get("/csrf")
        .expect(200)
        .expect(token -> {
          request().post("/csrf")
              .form()
              .add("csrf", token)
              .expect(200)
              .expect(newToken -> {
                assertEquals(token, newToken);
              });
        });
  }

  @Test
  public void csrfInvalid() throws Exception {
    request()
        .get("/csrf")
        .expect(200)
        .expect(token -> {
          request().post("/csrf")
              .form()
              .add("csrf", token + "1")
              .expect(403);
        });
  }
}
