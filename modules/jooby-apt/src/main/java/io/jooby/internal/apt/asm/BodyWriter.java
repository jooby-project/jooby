/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;

import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.jooby.internal.apt.MethodDescriptor;
import io.jooby.internal.apt.ParamDefinition;

public class BodyWriter extends ValueWriter {
  @Override
  public void accept(
      ClassWriter writer,
      Type controller,
      String handlerInternalName,
      MethodVisitor visitor,
      ParamDefinition parameter,
      NameGenerator nameGenerator)
      throws Exception {
    MethodDescriptor paramMethod = parameter.getObjectValue();
    if (parameter.is(byte[].class)) {
      visitor.visitMethodInsn(
          INVOKEINTERFACE,
          paramMethod.getDeclaringType().getInternalName(),
          paramMethod.getName(),
          paramMethod.getDescriptor(),
          true);
      visitor.visitMethodInsn(
          INVOKEINTERFACE,
          "io/jooby/Body",
          MethodDescriptor.Body.bytes().getName(),
          MethodDescriptor.Body.bytes().getDescriptor(),
          true);
    } else if (parameter.is(InputStream.class)) {
      visitor.visitMethodInsn(
          INVOKEINTERFACE,
          paramMethod.getDeclaringType().getInternalName(),
          paramMethod.getName(),
          paramMethod.getDescriptor(),
          true);
      visitor.visitMethodInsn(
          INVOKEINTERFACE,
          "io/jooby/Body",
          MethodDescriptor.Body.stream().getName(),
          MethodDescriptor.Body.stream().getDescriptor(),
          true);
    } else if (parameter.is(ReadableByteChannel.class)) {
      visitor.visitMethodInsn(
          INVOKEINTERFACE,
          paramMethod.getDeclaringType().getInternalName(),
          paramMethod.getName(),
          paramMethod.getDescriptor(),
          true);
      visitor.visitMethodInsn(
          INVOKEINTERFACE,
          "io/jooby/Body",
          MethodDescriptor.Body.channel().getName(),
          MethodDescriptor.Body.channel().getDescriptor(),
          true);
    } else if (parameter.is(String.class)) {
      visitor.visitMethodInsn(
          INVOKEINTERFACE,
          paramMethod.getDeclaringType().getInternalName(),
          paramMethod.getName(),
          paramMethod.getDescriptor(),
          true);
      MethodDescriptor methodDescriptor;
      if (parameter.isNullable()) {
        methodDescriptor = MethodDescriptor.Value.valueOrNull();
      } else {
        methodDescriptor = MethodDescriptor.Value.value();
      }
      visitor.visitMethodInsn(
          INVOKEINTERFACE,
          "io/jooby/Value",
          methodDescriptor.getName(),
          methodDescriptor.getDescriptor(),
          true);
    } else {
      MethodDescriptor convertMethod = parameter.getMethod();
      if (!convertMethod.getName().equals("body")) {
        visitor.visitMethodInsn(
            INVOKEINTERFACE,
            paramMethod.getDeclaringType().getInternalName(),
            paramMethod.getName(),
            paramMethod.getDescriptor(),
            true);
      }
      super.accept(writer, controller, handlerInternalName, visitor, parameter, nameGenerator);
    }
  }
}
