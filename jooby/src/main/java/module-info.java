/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby {
  exports io.jooby;
  exports io.jooby.annotations;
  
  /*
   * True core deps
   */
  requires jakarta.inject;
  requires org.slf4j;
  requires com.github.spotbugs.annotations;
  requires typesafe.config;
  requires java.management;
  
  /*
   * These are shaded and will have to be removed.
   */
  requires static org.apache.commons.io;
  requires static org.objectweb.asm;
  requires static org.objectweb.asm.tree;
  requires static org.objectweb.asm.tree.analysis;
  requires static org.objectweb.asm.util;
  requires static unbescape;
  
  /*
   * These reactive ones should be replaced with java 9 Flow
   * and or moved to a new module
   */
  requires static io.reactivex.rxjava2;
  requires static org.reactivestreams;
  requires static reactor.core;
  
  /*
   * Optional depedency for rate limiting
   */
  requires static io.github.bucket4j.core;

  /*
   * Kotlins dependencies are optional.
   */
  requires static kotlinx.coroutines.core.jvm;
  requires static kotlin.stdlib;

}