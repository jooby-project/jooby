/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;

public class RoutePatternIdioms extends Jooby {

  {
    final String pattern = "/variable";
    get(
        pattern,
        ctx -> {
          return "...";
        });

    delete(
        pattern + "/{id}",
        ctx -> {
          return "...";
        });

    final String subpath = "/foo";
    path(
        pattern,
        () -> {
          post(
              subpath,
              ctx -> {
                return "...";
              });

          path(
              pattern,
              () -> {
                put(
                    subpath,
                    ctx -> {
                      return "...";
                    });
              });
        });
  }

  public static void main(String[] args) {
    runApp(args, RoutePatternIdioms::new);
  }
}
