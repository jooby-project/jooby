/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/** Avaje Inject module. */
module io.jooby.avaje.inject {
  exports io.jooby.avaje.inject;

  requires transitive io.jooby;
  requires transitive typesafe.config;
  requires transitive io.avaje.inject;
}
