package examples;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.Route;

public class RouteIdioms extends Jooby {

  {
    path("/aaa", () -> {
      get("/bbb", ctx -> "some");
      path("/ccc", () -> {
        get("/ddd", ctx -> "some");
      });

      get("/eee", ctx -> "some");
    });

    get("/inline", ctx -> {
      return "...";
    });

    get("/routeReference", this::routeReference);

    get("/staticRouteReference", RouteIdioms::staticRouteReference);

    ExternalReference externalReference = new ExternalReference();
    get("/externalReference", externalReference::routeReference);

    get("/externalStaticReference", ExternalReference::externalStaticReference);

    HandlerA handler = new HandlerA();
    get("/alonevar", handler);

    get("/aloneinline", new HandlerA());

    Route.Handler h = ctx -> {
      return null;
    };
    get("/lambdaRef", h);
  }

  public String routeReference(Context ctx) {
    return ctx.toString();
  }

  public static String staticRouteReference(Context ctx) {
    return ctx.toString();
  }

  public static void main(String[] args) {
    runApp(args, RouteIdioms::new);
  }
}
