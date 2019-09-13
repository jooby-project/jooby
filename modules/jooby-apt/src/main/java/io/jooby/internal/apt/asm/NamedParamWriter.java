/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import io.jooby.internal.apt.ParamDefinition;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Type.getMethodDescriptor;

public class NamedParamWriter extends ValueWriter {

  private boolean tryName;

  public NamedParamWriter(boolean tryName) {
    this.tryName = tryName;
  }

  @Override
  public void accept(ClassWriter writer, String handlerInternalName, MethodVisitor visitor,
      ParamDefinition parameter) throws Exception {
    if (tryName) {
      param(writer, parameter);
      Method paramMethod = parameter.getObjectValue();
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);
      visitor.visitLdcInsn(parameter.getHttpName());
      visitor.visitMethodInsn(INVOKESTATIC, handlerInternalName, parameter.getName(),
          "(Lio/jooby/Value;Ljava/lang/String;)Lio/jooby/Value;", false);
    } else {
      Method paramMethod = parameter.getSingleValue();
      visitor.visitLdcInsn(parameter.getHttpName());
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);
    }

    super.accept(writer, handlerInternalName, visitor, parameter);
  }

  private void param(ClassWriter writer, ParamDefinition parameter) {
    MethodVisitor methodVisitor = writer.visitMethod(ACC_PRIVATE | ACC_STATIC, parameter.getName(),
        "(Lio/jooby/Value;Ljava/lang/String;)Lio/jooby/Value;", null, null);
    methodVisitor.visitParameter("scope", 0);
    methodVisitor.visitParameter("name", 0);
    methodVisitor.visitCode();
    Label label0 = new Label();
    methodVisitor.visitLabel(label0);
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitVarInsn(ALOAD, 1);
    methodVisitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Value", "get",
        "(Ljava/lang/String;)Lio/jooby/Value;", true);
    methodVisitor.visitVarInsn(ASTORE, 2);
    Label label1 = new Label();
    methodVisitor.visitLabel(label1);
    methodVisitor.visitVarInsn(ALOAD, 2);
    methodVisitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Value", "isMissing", "()Z", true);
    Label label2 = new Label();
    methodVisitor.visitJumpInsn(IFEQ, label2);
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Value", "size", "()I", true);
    methodVisitor.visitJumpInsn(IFLE, label2);
    methodVisitor.visitVarInsn(ALOAD, 0);
    Label label3 = new Label();
    methodVisitor.visitJumpInsn(GOTO, label3);
    methodVisitor.visitLabel(label2);
    methodVisitor.visitFrame(F_APPEND,1, new Object[] {"io/jooby/Value"}, 0, null);
    methodVisitor.visitVarInsn(ALOAD, 2);
    methodVisitor.visitLabel(label3);
    methodVisitor.visitFrame(F_SAME1, 0, null, 1, new Object[] {"io/jooby/Value"});
    methodVisitor.visitInsn(ARETURN);
    Label label4 = new Label();
    methodVisitor.visitLabel(label4);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }
}
