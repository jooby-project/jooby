/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i1859;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.apt.MvcModuleCompilerRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue1859 {

  @Test
  public void shouldGetNullOnMissingBody() throws Exception {
    new MvcModuleCompilerRunner(new C1859())
        .example(Expected1859.class)
        .module(
            app -> {
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext();
              ctx.setBody(new byte[0]);
              assertEquals("empty", router.post("/c/i1859", ctx).value().toString());
            });
  }
}
