/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import io.jooby.Context;
import io.jooby.internal.apt.ParamDefinition;
import io.jooby.internal.apt.ParamKind;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Map;

import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class ObjectTypeWriter implements ParamWriter {
  @Override
  public void accept(ClassWriter writer, Type controller,
      String handlerInternalName, MethodVisitor visitor,
      ParamDefinition parameter, Map<String, Integer> registry) throws Exception {
    if (!parameter.is(Context.class)) {
      Method method = ParamKind.forTypeInjection(parameter).valueObject(parameter);
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), method.getName(),
          Type.getMethodDescriptor(method), true);
    }
    if (parameter.isOptional()) {
      visitor.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "ofNullable",
          "(Ljava/lang/Object;)Ljava/util/Optional;", false);
    }
  }
}
