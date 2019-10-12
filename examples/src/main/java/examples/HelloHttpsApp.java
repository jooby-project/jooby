/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.SSLOptions;
import io.jooby.ServerOptions;

public class HelloHttpsApp extends Jooby {

  {
    ServerOptions options = new ServerOptions();
    options.setSecurePort(8443);
    options.setSsl(SSLOptions.x509());
    setServerOptions(options);

    get("/", ctx -> {
      return "Hello " + ctx.getScheme();
    });
  }

  public static void main(String[] args) {
    runApp(args, HelloHttpsApp::new);
  }
}
