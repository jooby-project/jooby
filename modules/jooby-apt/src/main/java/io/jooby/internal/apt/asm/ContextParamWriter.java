/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import io.jooby.internal.apt.ParamDefinition;
import io.jooby.internal.apt.Primitives;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;

public class ContextParamWriter extends ValueWriter {

  @Override
  public void accept(ClassWriter writer, String handlerInternalName, MethodVisitor visitor,
      ParamDefinition parameter, Map<String, Integer> registry) throws Exception {
    String methodName = parameter.getName();
    String name = parameter.getHttpName();

    visitor.visitLdcInsn(name);
    visitor.visitMethodInsn(INVOKESTATIC, handlerInternalName, methodName,
        "(Lio/jooby/Context;Ljava/lang/String;)Ljava/lang/Object;", false);
    if (parameter.getType().isPrimitive()) {
      Method toPrimitive = Primitives.toPrimitive(parameter.getType());
      visitor.visitTypeInsn(Opcodes.CHECKCAST, getInternalName(toPrimitive.getDeclaringClass()));
      visitor.visitMethodInsn(INVOKEVIRTUAL, getInternalName(toPrimitive.getDeclaringClass()),
          toPrimitive.getName(), getMethodDescriptor(toPrimitive), false);
    } else {
      visitor.visitTypeInsn(Opcodes.CHECKCAST, parameter.getType().toJvmType().getInternalName());
    }

    if (!registry.containsKey(methodName)) {
      attribute(writer, parameter, methodName);
      registry.put(methodName, 1);
    }
  }

  private void attribute(ClassWriter classWriter, ParamDefinition parameter, String methodName)
      throws NoSuchMethodException {
    MethodVisitor methodVisitor = classWriter
        .visitMethod(ACC_PRIVATE | ACC_STATIC, methodName,
            "(Lio/jooby/Context;Ljava/lang/String;)Ljava/lang/Object;",
            "<T:Ljava/lang/Object;>(Lio/jooby/Context;Ljava/lang/String;)TT;", null);
    methodVisitor.visitParameter("ctx", 0);
    methodVisitor.visitParameter("name", 0);
    methodVisitor.visitCode();
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitVarInsn(ALOAD, 1);
    Method attribute = parameter.getSingleValue();
    methodVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(attribute.getDeclaringClass()),
        attribute.getName(), getMethodDescriptor(attribute), true);

    // if (attribute == null)
    if (parameter.is(Map.class, String.class, Object.class)) {
      // return ctx.getAttributes();
      methodVisitor.visitVarInsn(ASTORE, 2);
      Label label1 = new Label();
      methodVisitor.visitLabel(label1);
      methodVisitor.visitVarInsn(ALOAD, 2);
      Label label2 = new Label();
      methodVisitor.visitJumpInsn(IFNONNULL, label2);
      Label label3 = new Label();
      methodVisitor.visitLabel(label3);
      methodVisitor.visitVarInsn(ALOAD, 0);
      Method attributes = parameter.getObjectValue();
      methodVisitor
          .visitMethodInsn(INVOKEINTERFACE, getInternalName(attributes.getDeclaringClass()),
              attributes.getName(),
              getMethodDescriptor(attributes), true);
      methodVisitor.visitInsn(ARETURN);
      methodVisitor.visitLabel(label2);
      methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/Object"}, 0, null);
      methodVisitor.visitVarInsn(ALOAD, 2);
    }
    methodVisitor.visitInsn(ARETURN);

    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }
}
