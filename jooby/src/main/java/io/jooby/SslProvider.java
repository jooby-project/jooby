/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.security.Provider;

/**
 * Allow to configure a custom SSLContext provider. Default SSL Context is JDK specific.
 *
 * OpenSSL (via Conscryt) is available as separated dependency (jooby-conscrypt).
 *
 * @author edgar.
 */
public interface SslProvider {

  /**
   * Provider name.
   *
   * @return Provider name.
   */
  String getName();

  /**
   * Creates a new provider.
   *
   * @return Creates a new provider.
   */
  Provider create();
}
