/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2408;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.jooby.apt.NewProcessorRunner;
import io.jooby.exception.MissingValueException;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue2408 {
  @Test
  public void shouldNotIgnoreAnnotationOnParam() throws Exception {
    new NewProcessorRunner(new C2408())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              assertEquals(
                  "nothing",
                  router
                      .get("/2408/nullable", new MockContext().setQueryString("?blah=stuff"))
                      .value());

              assertThrows(
                  MissingValueException.class,
                  () -> router.get("/2408/nonnull", new MockContext()).value());

              assertEquals(
                  "cool",
                  router
                      .get("/2408/nonnull", new MockContext().setQueryString("?name=cool"))
                      .value());
            });
  }
}
