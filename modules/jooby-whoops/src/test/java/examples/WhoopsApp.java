/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import java.nio.file.Path;
import java.nio.file.Paths;

import examples.utils.Utils;
import io.jooby.Jooby;
import io.jooby.whoops.WhoopsModule;

public class WhoopsApp extends Jooby {
  {
    get(
        "/{id}",
        ctx -> {
          Object id = ctx.path("id").intValue();
          return function(id);
        });

    install(new WhoopsModule(basedir()));
  }

  private Object function(Object id) {
    try {
      return innerFunction(id);
    } catch (Exception x) {
      throw new IllegalStateException("Something Happened", x);
    }
  }

  private Object innerFunction(Object id) {
    return Utils.fail(id);
  }

  public static void main(String[] args) {
    runApp(args, WhoopsApp::new);
  }

  private static Path basedir() {
    Path basedir = Paths.get(System.getProperty("user.dir"));
    if (!basedir.getFileName().toString().equals("jooby-whoops")) {
      // IDE vs Maven
      basedir = basedir.resolve("modules").resolve("jooby-whoops");
    }
    return basedir;
  }
}
