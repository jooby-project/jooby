/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.SSLHandler;
import io.jooby.ServerOptions;

public class HttpsApp extends Jooby {

  {
    before(new SSLHandler(true));
    setServerOptions(new ServerOptions().setSecurePort(8443));

    get("/", ctx -> {
      return ctx.getScheme() + "; secure: " + ctx.isSecure();
    });
  }

  public static void main(String[] args) {
    runApp(args, HttpsApp::new);
  }
}
