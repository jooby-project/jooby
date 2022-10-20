/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1580;

import io.jooby.Jooby;

public class App1580 extends Jooby {
  {
    mvc(Controller1580.class);
  }
}
