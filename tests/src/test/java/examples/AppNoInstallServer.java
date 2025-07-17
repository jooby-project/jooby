/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.jetty.JettyServer;

public class AppNoInstallServer extends Jooby {

  public static void main(String[] args) {
    runApp(args, new JettyServer(), AppNoInstallServer::new);
  }
}
