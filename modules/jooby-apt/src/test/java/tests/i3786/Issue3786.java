/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3786;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue3786 {
  @Test
  public void shouldCheckBase() throws Exception {
    new ProcessorRunner(new C3786())
        .withRouter(
            app -> {
              var router = new MockRouter(app);
              assertEquals("base", router.get("/inherited", new MockContext()).value());
              assertEquals(
                  "withPath", router.get("/inherited/withPath", new MockContext()).value());
              assertEquals("base: 123", router.get("/inherited/123", new MockContext()).value());
              assertEquals(
                  "GET/inherited/childOnly",
                  router.get("/inherited/childOnly", new MockContext()).value());
              assertEquals(
                  "POST/inherited/childOnly",
                  router.post("/inherited/childOnly", new MockContext()).value());
            });
  }

  @Test
  public void shouldCheckOverride() throws Exception {
    new ProcessorRunner(new D3786())
        .withRouter(
            app -> {
              var router = new MockRouter(app);
              assertEquals("base", router.get("/overrideMethod", new MockContext()).value());
              assertEquals(
                  "withPath", router.get("/overrideMethod/withPath", new MockContext()).value());
              assertEquals(
                  "base: 123", router.get("/overrideMethod/123", new MockContext()).value());
              assertEquals(
                  "/overrideMethod/childOnly",
                  router.get("/overrideMethod/childOnly", new MockContext()).value());
              assertEquals(
                  "/overrideMethod/user",
                  router.post("/overrideMethod/user", new MockContext()).value());
            });
  }
}
