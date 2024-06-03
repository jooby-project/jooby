/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2405;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.apt.NewProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue2405 {
  @Test
  public void shouldGenerateUniqueNames() throws Exception {
    new NewProcessorRunner(new C2405())
        .withRouter(
            app -> {
              app.converter(new Converter2405());
              MockRouter router = new MockRouter(app);
              assertEquals(
                  "foo",
                  router
                      .get("/2405/blah", new MockContext().setQueryString("?blah=foo"))
                      .value()
                      .toString());

              assertEquals(
                  "bar",
                  router
                      .get("/2405/blah2", new MockContext().setQueryString("?blah=bar"))
                      .value()
                      .toString());
            });
  }
}
