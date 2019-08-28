/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import io.jooby.internal.apt.ParamDefinition;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Type.getMethodDescriptor;

public class AnnotationParamWriter extends ValueWriter {
  @Override public void accept(ClassWriter writer, String handlerInternalName, MethodVisitor visitor, ParamDefinition parameter)
      throws Exception {
    String parameterName = parameter.getHttpName();

    Method paramMethod;
    if (parameter.isNamed()) {
      paramMethod = parameter.getSingleValue();
      visitor.visitLdcInsn(parameterName);
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);
    } else {
      paramMethod = parameter.getObjectValue();
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);
    }

    super.accept(writer, handlerInternalName, visitor, parameter);
  }

  private boolean is(Method method, int index, Class type) {
    Class<?>[] types = method.getParameterTypes();
    return index < types.length && types[index].equals(type);
  }

}
