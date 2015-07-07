package org.jooby.js;

import java.nio.file.Paths;

import org.jooby.internal.js.JsJooby;

public class JsApp {

  public static void main(final String[] args) throws Exception {
    new JsJooby().run(Paths.get("src", "test", "resources", "org", "jooby", "js", "app.js")
        .toFile()).start();
  }
}
