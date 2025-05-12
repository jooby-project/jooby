/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3654;

import io.jooby.Jooby;

public class App3654 extends Jooby {
  {
    mvc(Controller3654.class);
  }
}
