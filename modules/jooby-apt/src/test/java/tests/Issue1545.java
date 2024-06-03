/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.StatusCode;
import io.jooby.apt.ProcessorRunner;
import io.jooby.test.MockRouter;
import source.Controller1545;

public class Issue1545 {
  @Test
  public void shouldSetNoContentCodeForVoidRoute() throws Exception {
    new ProcessorRunner(new Controller1545())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);

              router.delete(
                  "/1545",
                  rsp -> {
                    assertEquals(StatusCode.NO_CONTENT, rsp.getStatusCode());
                  });

              router.delete(
                  "/1545/success",
                  rsp -> {
                    assertEquals(StatusCode.OK, rsp.getStatusCode());
                  });

              router.post(
                  "/1545",
                  rsp -> {
                    assertEquals(StatusCode.CREATED, rsp.getStatusCode());
                  });

              router.post(
                  "/1545/novoid",
                  rsp -> {
                    assertEquals(StatusCode.CREATED, rsp.getStatusCode());
                    assertEquals("OK", rsp.value());
                  });
            });
  }
}
