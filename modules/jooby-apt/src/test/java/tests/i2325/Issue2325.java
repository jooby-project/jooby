/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2325;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.apt.MvcModuleCompilerRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue2325 {
  @Test
  public void shouldFavorNamedParamWithCustomConverter() throws Exception {
    new MvcModuleCompilerRunner(new C2325())
        .example(Expected2325.class)
        .module(
            app -> {
              app.converter(new VC2325());
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext();
              ctx.setQueryString("?myId=1234_TODO");
              assertEquals("MyID:1234_TODO", router.get("/2325", ctx).value().toString());
            });
  }

  @Test
  public void shouldFavorObjectConverterWhenNamedArgIsMissing() throws Exception {
    new MvcModuleCompilerRunner(new C2325())
        .example(Expected2325.class)
        .module(
            app -> {
              app.converter(new VC2325());
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext();
              assertEquals("MyID:{}", router.get("/2325", ctx).value().toString());
            });
  }
}
