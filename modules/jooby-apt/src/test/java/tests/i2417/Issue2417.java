/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2417;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue2417 {
  @Test
  public void shouldNotIgnoreAnnotationOnParam() throws Exception {
    new ProcessorRunner(new C2417())
        .withRouter(
            (app, source) -> {
              System.out.println(source);
              MockRouter router = new MockRouter(app);
              assertEquals(
                  "2417",
                  router.get("/2417", new MockContext().setQueryString("?name=2417")).value());
            });
  }
}
