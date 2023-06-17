/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */

/**
 * Jooby Unit Test module.
 */
module io.jooby.test {
  exports io.jooby.test;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.junit.jupiter.api;
  requires org.slf4j;
}
