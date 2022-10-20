/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.Jooby;
import io.jooby.test.MockRouter;

public class Issue1637 {

  @Test
  public void shouldForwardRoute() {
    Jooby app = new Jooby();

    app.get("/foo/{var}", ctx -> ctx.forward("/bar/" + ctx.path("var").value()));

    app.get("/bar/{id}", ctx -> ctx.path("id").intValue());

    MockRouter router = new MockRouter(app);

    assertEquals(123, router.get("/foo/123").value());
  }
}
