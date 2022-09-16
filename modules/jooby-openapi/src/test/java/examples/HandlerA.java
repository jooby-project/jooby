package examples;

import io.jooby.Context;
import io.jooby.Route;

import edu.umd.cs.findbugs.annotations.NonNull;

public class HandlerA implements Route.Handler {
  @NonNull @Override public Object apply(@NonNull Context ctx) throws Exception {
    return null;
  }
}
