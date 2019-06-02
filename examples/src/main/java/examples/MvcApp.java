/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

public class MvcApp extends Jooby {

  {
    mvc(new PlainText());
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.EVENT_LOOP, MvcApp::new);
  }
}
