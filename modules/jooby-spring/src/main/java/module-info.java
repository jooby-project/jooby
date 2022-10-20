/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby.spring {
  exports io.jooby.spring;

  requires io.jooby;
  requires com.github.spotbugs.annotations;
  requires typesafe.config;
  requires jakarta.inject;
  requires spring.core;
  requires spring.beans;
  requires spring.context;
}
