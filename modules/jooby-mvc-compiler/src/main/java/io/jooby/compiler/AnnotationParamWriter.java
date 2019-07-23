package io.jooby.compiler;

import io.jooby.Reified;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Type.getMethodDescriptor;

class AnnotationParamWriter implements ParamWriter {
  @Override public void accept(ClassWriter writer, MethodVisitor visitor, ParamDefinition parameter)
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

    Method convertMethod = parameter.getMethod();
    // to(Class)
    boolean toClass = is(convertMethod, 0, Class.class);
    boolean toReified = is(convertMethod, 0, Reified.class);
    // toOptional(Class) or toList(Class) or toSet(Class)
    if (toClass) {
      visitor.visitLdcInsn(parameter.isGenericType()
          ? parameter.getByteCodeTypeArgument(0)
          : parameter.getByteCodeType()
      );
    } else if (toReified) {
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);

      visitor.visitLdcInsn(parameter.getByteCodeType());

      /** to(Reified): */
      Method reified = Reified.class
          .getDeclaredMethod(convertMethod.getName(), java.lang.reflect.Type.class);
      visitor.visitMethodInsn(INVOKESTATIC, "io/jooby/Reified", reified.getName(),
          getMethodDescriptor(reified), false);
    }

    visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Value", convertMethod.getName(),
        getMethodDescriptor(convertMethod), true);

    if (toClass || toReified) {
      visitor.visitTypeInsn(CHECKCAST, parameter.getByteCodeType().getInternalName());
    }

  }

  private boolean is(Method method, int index, Class type) {
    Class<?>[] types = method.getParameterTypes();
    return index < types.length && types[index].equals(type);
  }

}
