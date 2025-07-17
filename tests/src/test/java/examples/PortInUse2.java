/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import static io.jooby.Jooby.runApp;

import io.jooby.Context;
import io.jooby.ServerOptions;
import io.jooby.jetty.JettyServer;

public class PortInUse2 {
  public static void main(String[] args) {
    var options = new ServerOptions();
    //    options.setHttpsOnly(true);
    runApp(
        args,
        new JettyServer().setOptions(options),
        app -> {
          app.get("/", Context::getRequestPath);
        });
    System.out.println("Exited");
  }
}
