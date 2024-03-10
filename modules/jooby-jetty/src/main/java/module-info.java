/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
import io.jooby.Server;
import io.jooby.jetty.JettyServer;

/** Jetty module. */
module io.jooby.jetty {
  exports io.jooby.jetty;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.alpn.server;
  requires org.eclipse.jetty.http2.server;
  requires org.eclipse.jetty.websocket.server;

  provides Server with
      JettyServer;
}
