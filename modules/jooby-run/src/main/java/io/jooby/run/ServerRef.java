/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import io.jooby.Server;

import java.util.function.Consumer;

/**
 * This class is loaded as part of project classpath. JoobyRun sets a system property with the name
 * of this class. Jooby read the property and creates a new instance using reflection.
 *
 * Works as a callback for getting the server instance, you we can stop on code changes.
 *
 * This class must NOT be reference by any other class of this project. We use it dynamically via
 * reflection from application class path (not from joobyRun/maven/gradle classpath).
 *
 * Loading this class by reference it force the current classloader to load it and creates
 * conflicts with application class loader (jboss module).
 */
public class ServerRef implements Consumer<Server> {

  private static volatile Server server;

  @Override public void accept(Server server) {
    ServerRef.server = server;
  }

  /**
   * Stop the current running server.
   */
  public static void stop() {
    if (server != null) {
      try {
        server.stop();
      } finally {
        server = null;
      }
    }
  }
}
