/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty.http2;

import java.util.List;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;

public class JettyHttp2Configurer {

  private static final String H2 = "h2";
  private static final String H2_17 = "h2-17";
  private static final String HTTP_1_1 = "http/1.1";

  public List<ConnectionFactory> configure(HttpConfiguration input) {
    if (input.getCustomizer(SecureRequestCustomizer.class) != null) {
      ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory(H2, H2_17, HTTP_1_1);
      alpn.setDefaultProtocol(H2);

      HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(input);
      h2.setInitialStreamRecvWindow(1024 * 1024);
      h2.setInitialSessionRecvWindow(10 * 1024 * 1024);

      // FIX: Set Max Concurrent Streams higher if you have many bidi clients
      h2.setMaxConcurrentStreams(1000);
      return List.of(alpn, h2);
    } else {
      var h2c = new HTTP2CServerConnectionFactory(input);
      h2c.setInitialStreamRecvWindow(1024 * 1024);
      h2c.setInitialSessionRecvWindow(10 * 1024 * 1024);

      // FIX: Set Max Concurrent Streams higher if you have many bidi clients
      h2c.setMaxConcurrentStreams(1000);
      return List.of(h2c);
    }
  }
}
