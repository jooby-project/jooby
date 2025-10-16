package io.jooby.i3787;

import io.jooby.StatusCode;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3787 {

  private final MockRouter router = new MockRouter(new Application()).setFullExecution(true);

  @Test
  void test() {
    var ctx = new MockContext();
    router.tryError(new Application.CustomException(), ctx); // throws java.lang.NullPointerException
    assertEquals(StatusCode.BAD_REQUEST, ctx.getResponseCode());
  }
}
