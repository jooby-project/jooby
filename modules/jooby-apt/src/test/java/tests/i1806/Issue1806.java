/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i1806;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.jooby.apt.MvcModuleCompilerRunner;
import io.jooby.test.MockRouter;

public class Issue1806 {

  @Test
  public void shouldNotGetListWithNullValue() throws Exception {
    new MvcModuleCompilerRunner(new C1806())
        .module(
            app -> {
              MockRouter router = new MockRouter(app);
              assertEquals(Collections.emptyList(), router.get("/1806/c").value());
            });
  }
}
