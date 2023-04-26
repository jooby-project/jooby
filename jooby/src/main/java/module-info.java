/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby {
  exports io.jooby;
  exports io.jooby.annotation;
  exports io.jooby.exception;

  uses io.jooby.MvcFactory;
  uses io.jooby.Server;
  uses io.jooby.SslProvider;
  uses io.jooby.ResultHandler;

  /*
   * True core deps
   */
  requires jakarta.inject;
  requires org.slf4j;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires java.management;

  /*
   * Optional dependency for rate limiting
   */
  requires static io.github.bucket4j.core;

  // SHADED: All content after this line will be removed at build time
  requires static unbescape;
  requires kotlinx.coroutines.core.jvm;
  requires kotlin.stdlib;
}
