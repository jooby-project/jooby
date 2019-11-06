package io.jooby;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1405 {

  @Test
  public void issue1405() {
    new JoobyRunner(app -> {
      app.path("/recover", () -> {
        app.after((ctx, result, failure) -> {
          ctx.send(failure.getClass().getSimpleName());
        });
        app.get("/", ctx -> {
          throw new IllegalStateException("some error");
        });
      });

      app.path("/rethrow", () -> {
        app.after((ctx, result, failure) -> {
          ctx.setResetHeadersOnError(false);
          ctx.setResponseHeader("failure", failure.getClass().getSimpleName());
        });
        app.get("/", ctx -> {
          throw new IllegalStateException("some error");
        });
      });

      app.path("/suppressed", () -> {
        app.after((ctx, result, failure) -> {
          throw new IllegalArgumentException("x");
        });

        app.get("/", ctx -> {
          throw new IllegalStateException("some error");
        });
      });

      app.error((ctx, x, code) -> {
        ctx.setResponseCode(code);
        String errors = Stream.concat(Stream.of(x), Stream.of(x.getSuppressed()))
            .filter(Objects::nonNull)
            .map(it -> it.getClass().getSimpleName())
            .collect(Collectors.joining("::"));
        ctx.send("::" + errors);
      });
    }).ready(client -> {
      client.get("/recover", rsp -> {
        assertEquals("IllegalStateException", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("/rethrow", rsp -> {
        assertEquals("::IllegalStateException", rsp.body().string());
        assertEquals("IllegalStateException", rsp.header("failure"));
        assertEquals(500, rsp.code());
      });

      client.get("/suppressed", rsp -> {
        assertEquals("::IllegalStateException::IllegalArgumentException", rsp.body().string());
        assertEquals(500, rsp.code());
      });
    });
  }
}
