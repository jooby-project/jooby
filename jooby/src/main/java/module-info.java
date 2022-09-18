/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */

// BEGIN
// IMPORTANT this module-info VERSION IS ONLY for IDEs for Jooby Development
// IMPORTANT the actual module is src/main/java/module-info.shade
// IMPORTANT if you modify this file you might have to modify the other one
// END
module io.jooby {

  exports io.jooby;
  exports io.jooby.annotations;

  uses io.jooby.MvcFactory;
  uses io.jooby.Server;
  /*
   * True core deps
   */
  requires jakarta.inject;
  requires org.slf4j;
  requires com.github.spotbugs.annotations;
  requires typesafe.config;
  requires java.management;

  /*
   * These reactive ones should be replaced with java 9 Flow
   * and or moved to a new module
   */
  requires static io.reactivex.rxjava2;
  requires static org.reactivestreams;
  requires static reactor.core;

  /*
   * Optional dependency for rate limiting
   */
  requires static io.github.bucket4j.core;

  // BEGIN remove

  /*
   * These are shaded and will have to be removed.
   */
  requires static org.apache.commons.io;
  requires org.objectweb.asm;
  requires static org.objectweb.asm.tree;
  requires static org.objectweb.asm.tree.analysis;
  requires org.objectweb.asm.util;
  requires static unbescape;

  /*
   * Kotlins dependencies are optional.
   */
  requires kotlinx.coroutines.core.jvm;
  requires kotlin.stdlib;

  // END

}
