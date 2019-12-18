package io.jooby;

import io.jooby.exception.StatusCodeException;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1349 {

  public static class App1349 extends Jooby {
    {
      error(IllegalAccessException.class, ((ctx, cause, statusCode) -> {
        ctx.setResponseCode(statusCode);
        ctx.send(cause.getMessage());
      }));
      get("/1349", ctx ->
          something()
      );
      get("/1349/iae", ctx ->
          throwsIAE()
      );
    }

    private String throwsIAE() throws IllegalAccessException {
      throw new IllegalAccessException("no-access");
    }

    public String something() {
      throw new StatusCodeException(StatusCode.UNAUTHORIZED, "test");
    }

  }

  @ServerTest
  public void issue1349(ServerTestRunner runner) {
    runner.use(App1349::new)
        .ready(client -> {
          client.get("/1349", rsp -> {
            assertEquals(401, rsp.code());
          });
          client.get("/1349/iae", rsp -> {
            assertEquals(500, rsp.code());
            assertEquals("no-access", rsp.body().string());
          });
        });
  }
}
