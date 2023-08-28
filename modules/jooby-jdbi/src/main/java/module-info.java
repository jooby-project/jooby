/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/** JDBI module. */
module io.jooby.jdbi {
  exports io.jooby.jdbi;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires java.sql;
  requires org.jdbi.v3.core;
  requires jakarta.inject;
}
