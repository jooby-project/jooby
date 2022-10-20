/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1592;

import io.jooby.Jooby;

public class App1592 extends Jooby {

  {
    post("/nested", ctx -> ctx.body(FairData.class));
  }
}
