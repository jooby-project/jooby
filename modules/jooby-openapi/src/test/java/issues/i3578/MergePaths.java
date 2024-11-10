/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3578;

import io.jooby.Context;
import io.jooby.Jooby;

public class MergePaths extends Jooby {
  {
    get("/app-path", Context::getRequestPath);
  }
}
