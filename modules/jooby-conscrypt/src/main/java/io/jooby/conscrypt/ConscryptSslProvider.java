/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.conscrypt;

import java.security.Provider;

import org.conscrypt.OpenSSLProvider;

import io.jooby.SslProvider;

/**
 * OpenSSL/Conscrypt SSL provider.
 *
 * @author edgar
 * @since 2.9.6
 */
public class ConscryptSslProvider implements SslProvider {
  private static final String NAME = "Conscrypt";

  @Override public String getName() {
    return NAME;
  }

  @Override public Provider create() {
    return new OpenSSLProvider(NAME);
  }
}
