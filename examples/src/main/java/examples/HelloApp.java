/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.RouterOption;
import io.jooby.TraceHandler;
import io.jooby.banner.BannerModule;

public class HelloApp extends Jooby {

  {
    install(new BannerModule());

    decorator(new TraceHandler());

    setRouterOptions(RouterOption.IGNORE_TRAILING_SLASH);

    get("/", ctx -> {
      return "Hello World";
    });

    get("/foo/bar", ctx -> {
      return ctx.pathString() + "oo";
    });
    get("/foo/bar/", ctx -> {
      return ctx.pathString() + "oo/";
    });

    get("/foo/{bar}", ctx -> {
      return ctx.path("bar").value();
    });
  }

  public static void main(String[] args) {
    runApp(args, HelloApp::new);
  }
}
