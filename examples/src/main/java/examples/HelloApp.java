/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.RouterOptions;

import java.util.stream.Stream;

public class HelloApp extends Jooby {

  {
    Stream.of(getClass(), Jooby.class, getLog().getClass())
        .forEach(clazz -> {
          System.out.println(clazz.getName() + " loaded by: " + clazz.getClassLoader());
        });
    setRouterOptions(new RouterOptions().setCaseSensitive(false).setIgnoreTrailingSlash(false));

    get("/foo/bar", ctx -> {
      return ctx.pathString() + "oo";
    });

    get("/foo/{bar}", ctx -> {
      return ctx.path("bar").value();
    });
  }

  public static void main(String[] args) {
    runApp(args, HelloApp::new);
  }
}
