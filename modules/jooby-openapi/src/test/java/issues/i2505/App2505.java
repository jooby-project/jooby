/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2505;

import io.jooby.Jooby;

public class App2505 extends Jooby {
  {
    mvc(Controller2505.class);
  }
}
