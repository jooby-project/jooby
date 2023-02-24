/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.lang.reflect.Type;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import io.jooby.internal.apt.MethodDescriptor;
import io.jooby.internal.apt.ParamDefinition;

public abstract class ValueWriter implements ParamWriter {

  @Override
  public void accept(
      ClassWriter writer,
      org.objectweb.asm.Type controller,
      String handlerInternalName,
      MethodVisitor visitor,
      ParamDefinition parameter,
      NameGenerator nameGenerator)
      throws Exception {
    MethodDescriptor convertMethod = parameter.getMethod();
    // to(Class)
    boolean toClass = is(convertMethod, 0, Class.class);
    boolean toReified = is(convertMethod, 0, Type.class);
    // toOptional(Class) or toList(Class) or toSet(Class)
    if (toClass) {
      visitor.visitLdcInsn(
          parameter.getType().isParameterizedType()
              ? parameter.getType().getArguments().get(0).toJvmType()
              : parameter.getType().toJvmType());
    } else if (toReified) {
      MethodDescriptor reified;
      if (parameter.is(Map.class)) {
        visitor.visitLdcInsn(parameter.getType().getArguments().get(0).toJvmType());
        visitor.visitLdcInsn(parameter.getType().getArguments().get(1).toJvmType());
        reified = MethodDescriptor.Reified.map();
      } else {
        visitor.visitLdcInsn(parameter.getType().toJvmType());

        ArrayWriter.write(
            visitor,
            Type.class.getName(),
            parameter.getType().getArguments(),
            type -> visitor.visitLdcInsn(type.toJvmType()));
        reified = MethodDescriptor.Reified.getParameterized();
      }
      visitor.visitMethodInsn(
          INVOKESTATIC, "io/jooby/Reified", reified.getName(), reified.getDescriptor(), false);
      visitor.visitMethodInsn(
          INVOKEVIRTUAL,
          "io/jooby/Reified",
          MethodDescriptor.Reified.getType().getName(),
          MethodDescriptor.Reified.getType().getDescriptor(),
          false);
    }

    visitor.visitMethodInsn(
        INVOKEINTERFACE,
        convertMethod.getDeclaringType().getInternalName(),
        convertMethod.getName(),
        convertMethod.getDescriptor(),
        true);

    if (toClass || toReified) {
      visitor.visitTypeInsn(CHECKCAST, parameter.getType().toJvmType().getInternalName());
      if (!parameter.isNullable()) {
        visitor.visitVarInsn(ASTORE, 3);
        visitor.visitLdcInsn(parameter.getHttpName());
        visitor.visitVarInsn(ALOAD, 3);
        visitor.visitMethodInsn(
            INVOKESTATIC,
            "io/jooby/exception/MissingValueException",
            "requireNonNull",
            "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
            false);
        visitor.visitTypeInsn(CHECKCAST, parameter.getType().toJvmType().getInternalName());
      }
    }
  }

  private boolean is(MethodDescriptor method, int index, Class type) {
    org.objectweb.asm.Type[] types =
        org.objectweb.asm.Type.getArgumentTypes(method.getDescriptor());
    return index < types.length && types[index].getClassName().equals(type.getName());
  }
}
