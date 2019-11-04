/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.SslOptions;

import javax.net.ssl.SSLContext;

public interface SslContextProvider {

  boolean supports(String type);

  SSLContext create(ClassLoader loader, SslOptions options);

  static SslContextProvider[] providers() {
    return new SslContextProvider[] {new SslPkcs12Provider(), new SslX509Provider()};
  }
}
