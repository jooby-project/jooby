/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/** Flyway module. */
module io.jooby.flyway {
  exports io.jooby.flyway;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires java.sql;
  requires org.flywaydb.core;
}
