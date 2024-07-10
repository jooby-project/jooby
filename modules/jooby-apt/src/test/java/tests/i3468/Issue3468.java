/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3468;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue3468 {

  @Test
  public void shouldPrefixWithLeadingSlash() throws Exception {
    new ProcessorRunner(new C3468())
        .withRouter(
            (app, source) -> {
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext();
              assertEquals("/test", router.get("/test", ctx).value());
              assertTrue(source.toString().contains("app.get(\"/test\", this::test)"));
            });
  }
}
