/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue3472 {

  @Test
  public void shouldBindWithCustomCode() throws Exception {
    new ProcessorRunner(new C3472())
        .withRouter(
            app -> {
              var value = "xxx";
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext().setQueryString("?value=" + value);

              assertEquals(new BindBean(value), router.get("/3472", ctx).value());

              assertEquals(new BindBean("fn:" + value), router.get("/3472/named", ctx).value());

              assertEquals(
                  new BindBean("static:" + value), router.get("/3472/static", ctx).value());
            });
  }
}
