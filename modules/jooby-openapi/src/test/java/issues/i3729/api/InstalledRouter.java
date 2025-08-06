/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import io.jooby.Context;
import io.jooby.Jooby;

public class InstalledRouter extends Jooby {

  {
    /*
     * Installed operation.
     *
     * @operationId installedOp
     */
    get("/installed", Context::getRequestPath);

    mount(new MountedRouter());
  }
}
