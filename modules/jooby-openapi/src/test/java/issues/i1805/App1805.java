/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1805;

import io.jooby.Jooby;

public class App1805 extends Jooby {
  {
    mvc(C1805.class);
  }
}
