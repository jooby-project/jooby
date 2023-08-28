/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/** Metrics module. */
module io.jooby.metrics {
  exports io.jooby.metrics;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires com.codahale.metrics;
  requires com.codahale.metrics.health;
  requires com.codahale.metrics.jvm;
  requires java.management;
}
