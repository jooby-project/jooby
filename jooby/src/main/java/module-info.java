/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */

/** Jooby module. */
module io.jooby {
  exports io.jooby;
  exports io.jooby.annotation;
  exports io.jooby.buffer;
  exports io.jooby.exception;
  exports io.jooby.handler;
  exports io.jooby.validation;
  exports io.jooby.problem;

  uses io.jooby.MvcFactory;
  uses io.jooby.Server;
  uses io.jooby.SslProvider;
  uses io.jooby.LoggingService;
  uses io.jooby.buffer.DataBufferFactory;

  /*
   * True core deps
   */
  requires jakarta.inject;
  requires org.slf4j;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;

  /*
   * Optional dependency for rate limiting
   */
  requires static io.github.bucket4j.core;
}
