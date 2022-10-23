/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */

module io.jooby {
  exports io.jooby;
  exports io.jooby.annotations;
  exports io.jooby.exception;

  uses io.jooby.MvcFactory;
  uses io.jooby.Server;
  uses io.jooby.SslProvider;

  /*
   * True core deps
   */
  requires jakarta.inject;
  requires org.slf4j;
  requires com.github.spotbugs.annotations;
  requires typesafe.config;
  requires java.management;

  /*
   * TODO: These reactive ones should be replaced with java 9 Flow
   * and or moved to a new module
   */
  requires static io.reactivex.rxjava2;
  requires static org.reactivestreams;
  requires static reactor.core;

  /*
   * Optional dependency for rate limiting
   */
  requires static io.github.bucket4j.core;

  // SHADED: All content after this line will be removed at build time

  requires static org.apache.commons.io;
  requires org.objectweb.asm;
  requires static org.objectweb.asm.tree;
  requires static org.objectweb.asm.tree.analysis;
  requires org.objectweb.asm.util;
  requires static unbescape;
  requires kotlinx.coroutines.core.jvm;
  requires kotlin.stdlib;
}
