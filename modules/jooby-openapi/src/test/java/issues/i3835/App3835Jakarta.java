/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3835;

import io.jooby.Jooby;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

public class App3835Jakarta extends Jooby {
  {
    mvc(toMvcExtension(C3835Jakarta.class));
  }
}
