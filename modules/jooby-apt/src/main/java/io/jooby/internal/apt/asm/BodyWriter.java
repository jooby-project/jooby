/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import io.jooby.Body;
import io.jooby.Value;
import io.jooby.internal.apt.ParamDefinition;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;

import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Type.getMethodDescriptor;

public class BodyWriter extends ValueWriter {
  @Override
  public void accept(ClassWriter writer, Type controller,
      String handlerInternalName, MethodVisitor visitor,
      ParamDefinition parameter, Map<String, Integer> registry)
      throws Exception {
    Method paramMethod = parameter.getObjectValue();
    if (parameter.is(byte[].class)) {
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);
      Method bytes = Body.class.getDeclaredMethod("bytes");
      visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", bytes.getName(),
          getMethodDescriptor(bytes), true);
    } else if (parameter.is(InputStream.class)) {
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);
      Method stream = Body.class.getDeclaredMethod("stream");
      visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", stream.getName(),
          getMethodDescriptor(stream), true);
    } else if (parameter.is(ReadableByteChannel.class)) {
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);
      Method channel = Body.class.getDeclaredMethod("channel");
      visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", channel.getName(),
          getMethodDescriptor(channel), true);
    } else if (parameter.is(String.class)) {
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
          getMethodDescriptor(paramMethod), true);
      String methodName;
      if (parameter.isNullable()) {
        methodName = "valueOrNull";
      } else {
        methodName = "value";
      }
      Method value = Value.class.getDeclaredMethod(methodName);
      visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Value", value.getName(),
          getMethodDescriptor(value), true);
    } else {
      Method convertMethod = parameter.getMethod();
      if (!convertMethod.getName().equals("body")) {
        visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
            getMethodDescriptor(paramMethod), true);
      }
      super.accept(writer, controller, handlerInternalName, visitor, parameter, registry);
    }
  }
}
