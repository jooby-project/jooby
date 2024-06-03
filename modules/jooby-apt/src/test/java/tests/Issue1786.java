/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.jooby.StatusCode;
import io.jooby.apt.NewProcessorRunner;
import io.jooby.exception.MissingValueException;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;
import source.Controller1786;
import source.Controller1786b;

public class Issue1786 {

  @Test
  public void shouldThrowMissingValueExceptionIfRequiredStringParamNotSpecified() throws Exception {
    new NewProcessorRunner(new Controller1786())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              assertThrows(MissingValueException.class, () -> router.get("/required-string-param"));
            });
  }

  @Test
  public void shouldThrowMissingValueExceptionIfRequiredParamNotSpecified() throws Exception {
    new NewProcessorRunner(new Controller1786b())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              assertThrows(MissingValueException.class, () -> router.get("/required-param"));
            });
  }

  @Test
  public void shouldReturnValueIfRequiredStringParamSpecified() throws Exception {
    new NewProcessorRunner(new Controller1786())
        .withRouter(
            app -> {
              final String expectedValue = "non-null string";

              MockContext ctx = new MockContext();
              ctx.setQueryString("value=" + expectedValue);

              MockRouter router = new MockRouter(app);
              router.get(
                  "/required-string-param",
                  ctx,
                  rsp -> {
                    assertEquals(StatusCode.OK, rsp.getStatusCode());
                    assertEquals(expectedValue, rsp.value());
                  });
            });
  }
}
