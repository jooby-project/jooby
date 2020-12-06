/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static io.undertow.UndertowOptions.ENABLE_HTTP2;

import io.jooby.Http2Configurer;
import io.undertow.Undertow;

/**
 * This class is useless, due Undertow comes with built-in support for HTTP/2.
 *
 * We need it to normalize usage across web server implementation.
 */
public class UndertowHttp2Configurer implements Http2Configurer<Undertow.Builder, Void> {

  @Override public boolean support(Class type) {
    return Undertow.Builder.class == type;
  }

  @Override public Void configure(Undertow.Builder input) {
    input.setServerOption(ENABLE_HTTP2, true);
    return null;
  }
}
