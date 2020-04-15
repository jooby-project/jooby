/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;

public interface ContextInitializer {
  ContextInitializer PROXY_PEER_ADDRESS = ctx -> ProxyPeerAddress.parse(ctx).set(ctx);

  void apply(Context ctx);

  default ContextInitializer add(ContextInitializer initializer) {
    return initializer;
  }
}
