/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */

/** Reactor module */
module io.jooby.reactor {
  exports io.jooby.reactor;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires reactor.core;
  requires org.reactivestreams;
  requires org.slf4j;
}
