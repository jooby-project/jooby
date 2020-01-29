package examples;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class HandlerA implements Route.Handler {
  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    return null;
  }
}
