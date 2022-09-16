package io.jooby.apt;

import io.jooby.Context;
import io.jooby.Route;

import edu.umd.cs.findbugs.annotations.NonNull;

public class TestHandler implements Route.Handler {

  private Route.Handler next;

  public TestHandler(Route.Handler next) {
    this.next = next;
  }

  @NonNull @Override public Object apply(@NonNull Context ctx) throws Exception {
    return next.apply(ctx);
  }
}
