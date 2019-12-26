package io.jooby.apt;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class TestHandler implements Route.Handler {

  private Route.Handler next;

  public TestHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    return next.apply(ctx);
  }
}
