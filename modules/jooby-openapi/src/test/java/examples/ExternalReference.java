package examples;

import io.jooby.Context;

public class ExternalReference  {

  public String routeReference(Context ctx) {
    return ctx.toString();
  }

  public static String externalStaticReference(Context ctx) {
    return ctx.toString();
  }
}
