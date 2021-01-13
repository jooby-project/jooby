/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import javax.net.ssl.SSLEngine;

import org.conscrypt.Conscrypt;

import io.undertow.protocols.alpn.ALPNProvider;

public class ConscriptAlpnProvider implements ALPNProvider {
  @Override public boolean isEnabled(SSLEngine sslEngine) {
    return sslEngine.getClass().getName().startsWith("org.conscrypt.");
  }

  @Override public SSLEngine setProtocols(SSLEngine engine, String[] protocols) {
    Impl.setProtocols(engine, protocols);
    return engine;
  }

  @Override public String getSelectedProtocol(SSLEngine engine) {
    return Impl.getSelectedProtocol(engine);
  }

  @Override public int getPriority() {
    return 400;
  }

  private static class Impl {
    static SSLEngine setProtocols(SSLEngine engine, String[] protocols) {
      Conscrypt.setApplicationProtocols(engine, protocols);
      return engine;
    }

    static String getSelectedProtocol(SSLEngine engine) {
      return Conscrypt.getApplicationProtocol(engine);
    }
  }
}
