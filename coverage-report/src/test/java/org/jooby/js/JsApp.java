package org.jooby.js;

import java.io.File;
import java.nio.file.Paths;

import org.jooby.Jooby;
import org.jooby.JoobyJs;

public class JsApp {

  public static void main(final String[] args) throws Throwable {
    File appjs = Paths.get("src", "test", "resources", "org", "jooby", "js", "app.js")
        .toFile();
    Jooby.run(new JoobyJs().run(appjs), args);
  }
}
