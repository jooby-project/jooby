/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import examples.SubController;
import io.jooby.Jooby;

public class App1586 extends Jooby {
  {
    mvc(toMvcExtension(SubController.class));
  }
}
