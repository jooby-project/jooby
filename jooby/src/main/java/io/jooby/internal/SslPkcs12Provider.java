/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import io.jooby.SneakyThrows;
import io.jooby.SslOptions;

public class SslPkcs12Provider implements SslContextProvider {

  @Override public boolean supports(String type) {
    return SslOptions.PKCS12.equalsIgnoreCase(type);
  }

  @Override public SSLContext create(ClassLoader loader, String provider, SslOptions options) {
    try {
      KeyStore store = keystore(options, loader, options.getCert(), options.getPassword());
      KeyManagerFactory kmf = KeyManagerFactory
          .getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(store, toCharArray(options.getPassword()));
      KeyManager[] kms = kmf.getKeyManagers();
      SSLContext context = provider == null
          ? SSLContext.getInstance("TLS")
          : SSLContext.getInstance("TLS", provider);

      TrustManager[] tms;
      if (options.getTrustCert() != null) {
        KeyStore trustStore = keystore(options, loader, options.getTrustCert(),
            options.getTrustPassword());

        TrustManagerFactory tmf = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        tms = tmf.getTrustManagers();
      } else {
        tms = null;
      }

      context.init(kms, tms, null);
      return context;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private KeyStore keystore(SslOptions options, ClassLoader loader, String file, String password)
      throws Exception {
    try (InputStream crt = options.getResource(loader, file)) {
      KeyStore store = KeyStore.getInstance(options.getType());
      store.load(crt, toCharArray(password));
      return store;
    }
  }

  private char[] toCharArray(String password) {
    return password == null ? null : password.toCharArray();
  }
}
