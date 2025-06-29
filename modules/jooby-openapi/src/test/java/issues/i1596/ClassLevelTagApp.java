/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1596;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Jooby;

public class ClassLevelTagApp extends Jooby {
  {
    mvc(toMvcExtension(ClassLevelController.class));
  }
}
