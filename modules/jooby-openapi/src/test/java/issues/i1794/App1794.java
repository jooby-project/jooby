/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1794;

import io.jooby.Jooby;

public class App1794 extends Jooby {

  {
    mvc(new Controller1794());
  }
}
