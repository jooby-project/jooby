/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.apt.NewProcessorRunner;
import io.jooby.test.MockRouter;
import source.EmptySubClassController;
import source.OverrideMethodSubClassController;
import source.SubController;

public class InheritanceTest {

  @Test
  public void shouldWorkWithSubClass() throws Exception {
    new NewProcessorRunner(new SubController())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              router.get(
                  "/base",
                  rsp -> {
                    assertEquals("base", rsp.value());
                  });
              router.get(
                  "/base/withPath",
                  rsp -> {
                    assertEquals("withPath", rsp.value());
                  });
              router.get(
                  "/base/subPath",
                  rsp -> {
                    assertEquals("subPath", rsp.value());
                  });
            });
  }

  @Test
  public void shouldProcessEmptySubclasses() throws Exception {
    new NewProcessorRunner(new EmptySubClassController())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              router.get(
                  "/override",
                  rsp -> {
                    assertEquals("base", rsp.value());
                  });
              router.get(
                  "/override/withPath",
                  rsp -> {
                    assertEquals("withPath", rsp.value());
                  });
            });
  }

  @Test
  public void shouldProcessWithOverrideMethodSubclasses() throws Exception {
    new NewProcessorRunner(new OverrideMethodSubClassController())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              router.get(
                  "/overrideMethod",
                  rsp -> {
                    assertEquals("base", rsp.value());
                  });
              router.get(
                  "/overrideMethod/newpath",
                  rsp -> {
                    assertEquals("withPath", rsp.value());
                  });
            });
  }
}
