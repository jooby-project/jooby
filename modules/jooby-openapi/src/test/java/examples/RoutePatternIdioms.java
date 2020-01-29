package examples;

import io.jooby.Jooby;

public class RoutePatternIdioms extends Jooby {

  {
    final String pattern = "/variable";
    get(pattern, ctx -> {
      return "...";
    });

    get(pattern + "/{id}", ctx -> {
      return "...";
    });

    final String subpath = "/foo";
    path(pattern, () -> {
      get(subpath, ctx -> {
        return "...";
      });

      path(pattern, () -> {
        get(subpath, ctx -> {
          return "...";
        });
      });
    });
  }

  public static void main(String[] args) {
    runApp(args, RoutePatternIdioms::new);
  }
}
