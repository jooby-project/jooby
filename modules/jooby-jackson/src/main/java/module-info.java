/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby.jackson {
  exports io.jooby.jackson;

  requires io.jooby;
  requires com.github.spotbugs.annotations;
  requires typesafe.config;

  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires com.fasterxml.jackson.module.paramnames;
}
