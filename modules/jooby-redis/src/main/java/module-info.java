/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby.redis {
  exports io.jooby.redis;

  requires io.jooby;
  requires com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires org.apache.commons.pool2;
  requires lettuce.core;
}
