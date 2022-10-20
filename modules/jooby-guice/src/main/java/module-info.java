/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby.guice {
  exports io.jooby.guice;

  requires io.jooby;
  requires com.github.spotbugs.annotations;
  requires typesafe.config;
  requires com.google.guice;
  requires jakarta.inject;
  requires javax.inject;
}
