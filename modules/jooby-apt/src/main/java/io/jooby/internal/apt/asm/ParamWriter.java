/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import io.jooby.Context;
import io.jooby.internal.apt.ParamDefinition;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Map;

import static org.objectweb.asm.Type.getType;

public interface ParamWriter {
  Type CTX = getType(Context.class);

  void accept(ClassWriter writer, Type controller, String handlerInternalName,
      MethodVisitor visitor,
      ParamDefinition parameter, Map<String, Integer> registry) throws Exception;
}
