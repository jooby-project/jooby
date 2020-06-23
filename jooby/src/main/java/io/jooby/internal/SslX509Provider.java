/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.SneakyThrows;
import io.jooby.SslOptions;
import io.jooby.internal.x509.SslContext;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

public class SslX509Provider implements SslContextProvider {
  @Override public boolean supports(String type) {
    return SslOptions.X509.equalsIgnoreCase(type);
  }

  @Override public SSLContext create(ClassLoader loader, SslOptions options) {
    try {
      InputStream trustCert;
      if (options.getTrustCert() == null) {
        trustCert = null;
      } else {
        trustCert = options.getResource(loader, options.getTrustCert());
      }
      InputStream keyStoreCert = options.getResource(loader, options.getCert());
      InputStream keyStoreKey = options.getResource(loader, options.getPrivateKey());
      String keyStorePass = null;

      SSLContext context = SslContext
          .newServerContextInternal(trustCert, keyStoreCert, keyStoreKey, keyStorePass, 0, 0)
          .context();

      return context;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
