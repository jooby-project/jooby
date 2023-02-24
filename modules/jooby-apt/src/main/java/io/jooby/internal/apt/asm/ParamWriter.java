/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.jooby.internal.apt.ParamDefinition;

public interface ParamWriter {
  void accept(
      ClassWriter writer,
      Type controller,
      String handlerInternalName,
      MethodVisitor visitor,
      ParamDefinition parameter,
      NameGenerator nameGenerator)
      throws Exception;
}
