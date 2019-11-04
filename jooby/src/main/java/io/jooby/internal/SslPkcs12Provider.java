/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.SslOptions;
import io.jooby.SneakyThrows;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SslPkcs12Provider implements SslContextProvider {

  @Override public boolean supports(String type) {
    return SslOptions.PKCS12.equalsIgnoreCase(type);
  }

  @Override public SSLContext create(ClassLoader loader, SslOptions options) {
    try (InputStream crt = options.getResource(loader, options.getCert())) {
      KeyStore store = KeyStore.getInstance(options.getType());
      store.load(crt, options.getPassword().toCharArray());
      KeyManagerFactory kmf = KeyManagerFactory
          .getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(store, options.getPassword().toCharArray());
      KeyManager[] kms = kmf.getKeyManagers();
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(kms, null, SecureRandom.getInstanceStrong());
      return context;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
