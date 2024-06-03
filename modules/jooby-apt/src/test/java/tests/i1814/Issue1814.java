/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i1814;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.apt.NewProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue1814 {

  @Test
  public void shouldIgnoreWildcardResponseType() throws Exception {
    new NewProcessorRunner(new C1814())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext();
              ctx.setQueryString("?type=foo");
              assertEquals("[foo]", router.get("/1814", ctx).value().toString());
            });
  }
}
