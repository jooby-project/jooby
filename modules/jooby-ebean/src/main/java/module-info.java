/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby.ebean {
  exports io.jooby.ebean;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires io.ebean;
}
