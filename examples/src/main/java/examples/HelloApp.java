/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.RouterOptions;
import io.jooby.TraceHandler;
import io.jooby.annotations.QueryParam;
import io.jooby.banner.BannerModule;

import java.util.stream.Stream;

public class HelloApp extends Jooby {

  {
    install(new BannerModule());

    decorator(new TraceHandler());

    setRouterOptions(new RouterOptions().setIgnoreCase(false).setIgnoreTrailingSlash(true));

    get("/", ctx -> {
      return ctx.pathString() + "oo";
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
