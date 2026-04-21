/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.ws;

public enum WsLifecycle {
  CONNECT,
  MESSAGE,
  CLOSE,
  ERROR
}
