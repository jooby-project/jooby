/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import io.jooby.Context;
import io.jooby.Jooby;

public class MountedApp extends Jooby {
  {
    /*
     * This is the main router.
     */
    get("/main", Context::getRequestPath);

    mount(new MountedRouter());

    mount("/mount-point", new MountedRouter());

    install(InstalledRouter::new);

    install("/install-point", InstalledRouter::new);
  }
}
