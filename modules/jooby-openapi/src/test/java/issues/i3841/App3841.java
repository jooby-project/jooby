/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3841;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Jooby;

/** App with or without doc. Description doc. */
public class App3841 extends Jooby {
  {
    mvc(toMvcExtension(C3841.class));
  }
}
