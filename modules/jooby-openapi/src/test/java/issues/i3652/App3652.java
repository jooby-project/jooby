/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3652;

import io.jooby.Jooby;

public class App3652 extends Jooby {
  {
    mvc(Controller3652.class);
  }
}
