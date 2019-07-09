/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import java.lang.reflect.Method;
import java.util.function.Consumer;

class ServerRef implements Consumer<Object> {

  private static volatile Object server;

  @Override public void accept(Object server) {
    ServerRef.server = server;
  }

  public static void stopServer() throws Exception {
    if (server != null) {
      try {
        Method stop = server.getClass().getDeclaredMethod("stop");
        stop.invoke(server);
      } finally {
        server = null;
      }
    }
  }
}
