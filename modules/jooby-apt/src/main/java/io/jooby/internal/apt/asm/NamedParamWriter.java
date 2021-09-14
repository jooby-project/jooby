/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Type.getMethodDescriptor;

import java.lang.reflect.Method;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.jooby.internal.apt.ParamDefinition;

public class NamedParamWriter extends ValueWriter {

  @Override
  public void accept(ClassWriter writer, Type controller,
      String handlerInternalName, MethodVisitor visitor,
      ParamDefinition parameter, NameGenerator nameGenerator) throws Exception {
    String parameterName = parameter.getHttpName();

    Method paramMethod;
    if (parameter.isNamed()) {
      paramMethod = parameter.getSingleValue();
      visitor.visitLdcInsn(parameterName);
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);

      super.accept(writer, controller, handlerInternalName, visitor, parameter, nameGenerator);
    } else {
      // Type getParamName(Context):
      String methodName =  nameGenerator.generate("$lookup", parameter.getName());
      String descriptor = "(Lio/jooby/Context;)" + parameter.getType().toJvmType().getDescriptor();
      visitor.visitMethodInsn(Opcodes.INVOKESTATIC, handlerInternalName, methodName, descriptor,
          false);

      lookupParam(writer, controller, handlerInternalName, methodName, descriptor, parameter,
          nameGenerator);
    }
  }

  /**
   * This method look for named parameter, if present favor single value assuming there is a
   * custom converter for it. Otherwise, fallback to bean converter:
   *
   * <pre>{@code
   * private static MyID2325 lookupMyId(io.jooby.Context ctx) {
   *     return !ctx.query("myId").isMissing()
   *         ? ctx.query("myId").to(MyID2325.class)
   *         : ctx.query().to(MyID2325.class);
   *   }
   * }</pre>
   *
   * @param writer
   * @param controller
   * @param handlerInternalName
   * @param methodName
   * @param descriptor
   * @param parameter
   * @param registry
   * @throws Exception
   */
  private void lookupParam(ClassWriter writer, Type controller, String handlerInternalName,
      String methodName, String descriptor, ParamDefinition parameter,
      NameGenerator registry) throws Exception {
    String paramName = parameter.getHttpName();
    Method paramMethod = parameter.getSingleValue();
    MethodVisitor methodVisitor = writer
        .visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName,
            descriptor, null, null);
    methodVisitor.visitParameter("ctx", ACC_SYNTHETIC);
    methodVisitor.visitCode();
    Label label0 = new Label();
    methodVisitor.visitLabel(label0);

    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitLdcInsn(paramName);
    methodVisitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
        getMethodDescriptor(paramMethod), true);
    methodVisitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/ValueNode", "isMissing", "()Z", true);
    Label label1 = new Label();
    methodVisitor.visitJumpInsn(Opcodes.IFEQ, label1);
    methodVisitor.visitVarInsn(ALOAD, 0);

    Label label2 = new Label();
    methodVisitor.visitLabel(label2);

    Method objectValue = parameter.getObjectValue();
    methodVisitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), objectValue.getName(),
        getMethodDescriptor(objectValue), true);

    super.accept(writer, controller, handlerInternalName, methodVisitor, parameter, registry);

    Label label3 = new Label();
    methodVisitor.visitJumpInsn(Opcodes.GOTO, label3);
    methodVisitor.visitLabel(label1);
    methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitLdcInsn(paramName);
    Label label4 = new Label();
    methodVisitor.visitLabel(label4);

    methodVisitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
        getMethodDescriptor(paramMethod), true);

    super.accept(writer, controller, handlerInternalName, methodVisitor, parameter, registry);

    methodVisitor.visitLabel(label3);
    methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1,
        new Object[]{parameter.getType().toJvmType().getInternalName()});
    methodVisitor.visitInsn(Opcodes.ARETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }
}
