/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.json.JacksonModule;

public class MvcApp extends Jooby {

  {
    install(new JacksonModule());

    mvc(new MvcController());
  }

  public static void main(String[] args) {
    runApp(args, MvcApp::new);
  }
}
