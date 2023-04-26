/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby.hikari {
  exports io.jooby.hikari;

  requires io.jooby;
  requires org.slf4j;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires java.sql;
  requires com.zaxxer.hikari;
}
