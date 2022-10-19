/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
import io.jooby.Http2Configurer;
import io.jooby.internal.jetty.JettyHttp2Configurer;

module io.jooby.http2.jetty {
  requires io.jooby;
  requires org.eclipse.jetty.alpn.server;
  requires org.eclipse.jetty.http2.server;

  provides Http2Configurer with JettyHttp2Configurer;
}
