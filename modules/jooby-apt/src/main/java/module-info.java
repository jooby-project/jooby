/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby.apt {
  exports io.jooby.apt;

  requires io.jooby;
  requires java.compiler;
  requires com.github.spotbugs.annotations;

  requires jakarta.inject;

  requires org.objectweb.asm;
  requires static org.objectweb.asm.tree;
  requires static org.objectweb.asm.tree.analysis;
  requires org.objectweb.asm.util;
}
