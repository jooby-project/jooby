/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import javax.net.ssl.SSLContext;

import io.jooby.SneakyThrows;
import io.jooby.SslOptions;
import io.jooby.internal.x509.SslContext;

public class SslX509Provider implements SslContextProvider {
  @Override
  public boolean supports(String type) {
    return SslOptions.X509.equalsIgnoreCase(type);
  }

  @Override
  public SSLContext create(ClassLoader loader, String provider, SslOptions options) {
    try (options) {
      SslContext sslContext =
          SslContext.newServerContextInternal(
              provider,
              options.getTrustCert(),
              options.getCert(),
              options.getPrivateKey(),
              null,
              0,
              0);

      return sslContext.context();
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
