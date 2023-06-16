/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i2956;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.exception.BadRequestException;
import io.jooby.test.MockRouter;

public class Issue2956 {

  @Test
  public void shouldTryError() {
    var app = new Jooby();

    app.get(
        "/2956",
        ctx -> {
          return ctx.query("q").value();
        });

    app.error(
        BadRequestException.class,
        (ctx, cause, code) -> {
          app.getLog().error("Exception: {}", cause.getClass().getSimpleName());
          ctx.setResponseCode(code);
          ctx.render(cause.getMessage());
        });

    app.error(
        (ctx, cause, code) -> {
          app.getLog().error("Fallback: {}", cause.getClass().getSimpleName());
          ctx.send(code);
        });

    var router = new MockRouter(app);

    router.tryError(
        new BadRequestException("Missing q"),
        rsp -> {
          assertEquals(StatusCode.BAD_REQUEST, rsp.getStatusCode());
          assertEquals("Missing q", rsp.value());
        });

    var ctx = mock(Context.class);
    router.tryError(new IllegalArgumentException("Message"), ctx);
    verify(ctx).send(StatusCode.BAD_REQUEST);
  }
}
