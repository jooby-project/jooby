package io.jooby.compiler;

import io.jooby.Body;
import io.jooby.Reified;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.channels.ReadableByteChannel;

import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Type.getMethodDescriptor;

class BodyWriter implements ParamWriter {
  @Override public void accept(ClassWriter writer, MethodVisitor visitor, ParamDefinition parameter)
      throws Exception {
    Method paramMethod = parameter.getObjectValue();
    visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
        getMethodDescriptor(paramMethod), true);
    if (parameter.is(byte[].class)) {
      Method bytes = Body.class.getDeclaredMethod("bytes");
      visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", bytes.getName(),
          getMethodDescriptor(bytes), true);
    } else if (parameter.is(InputStream.class)) {
      Method stream = Body.class.getDeclaredMethod("stream");
      visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", stream.getName(),
          getMethodDescriptor(stream), true);
    } else if (parameter.is(ReadableByteChannel.class)) {
      Method channel = Body.class.getDeclaredMethod("channel");
      visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", channel.getName(),
          getMethodDescriptor(channel), true);
    } else {
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

      visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", convertMethod.getName(),
          getMethodDescriptor(convertMethod), true);

      if (toClass || toReified) {
        visitor.visitTypeInsn(CHECKCAST, parameter.getByteCodeType().getInternalName());
      }
    }
  }

  private boolean is(Method method, int index, Class type) {
    Class<?>[] types = method.getParameterTypes();
    return index < types.length && types[index].equals(type);
  }
}
