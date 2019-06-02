/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AssetsApp extends Jooby {

  {
    Path www = Paths.get(System.getProperty("user.dir"), "examples", "www");
    assets("/*", www);
    assets("/static/*", www);
  }

  public static void main(String[] args) {
    runApp(args, AssetsApp::new);
  }
}
