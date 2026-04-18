/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */

/** Yasson module. */
module io.jooby.yasson {
  exports io.jooby.yasson;

  requires io.jooby;
  requires static org.jspecify;
  requires typesafe.config;
  requires jakarta.json.bind;
}
