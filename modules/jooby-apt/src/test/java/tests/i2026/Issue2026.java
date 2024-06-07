/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2026;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue2026 {

  @Test
  public void shouldGenerateWriteMethodSignature() throws Exception {
    new ProcessorRunner(new C2026())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext();
              assertEquals("TODO...", router.get("/api/todo", ctx).value().toString());
            });
  }
}
