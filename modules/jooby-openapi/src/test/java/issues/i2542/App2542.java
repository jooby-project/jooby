/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2542;

import io.jooby.Jooby;

public class App2542 extends Jooby {
  {
    mvc(Controller2542.class);
  }
}
