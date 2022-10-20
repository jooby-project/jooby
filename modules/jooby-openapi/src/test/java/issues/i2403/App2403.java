/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2403;

import io.jooby.Jooby;

public class App2403 extends Jooby {
  {
    mvc(Controller2403Copy.class);
  }
}
