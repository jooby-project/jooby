/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import examples.EmptySubClassController;
import io.jooby.Jooby;

public class App1586b extends Jooby {
  {
    mvc(toMvcExtension(EmptySubClassController.class));
  }
}
