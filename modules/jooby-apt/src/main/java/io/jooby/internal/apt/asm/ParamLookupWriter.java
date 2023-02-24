/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.jooby.internal.apt.JoobyTypes;
import io.jooby.internal.apt.MethodDescriptor;
import io.jooby.internal.apt.ParamDefinition;

public class ParamLookupWriter extends ValueWriter {

  @Override
  public void accept(
      ClassWriter writer,
      Type controller,
      String handlerInternalName,
      MethodVisitor visitor,
      ParamDefinition parameter,
      NameGenerator nameGenerator)
      throws Exception {

    String parameterName = parameter.getHttpName();
    String[] sources = parameter.sources();
    MethodDescriptor paramMethod = parameter.getSingleValue();
    String internalName = JoobyTypes.ParamSource.getInternalName();

    visitor.visitLdcInsn(parameterName);
    pushInt(sources.length, visitor);
    visitor.visitTypeInsn(ANEWARRAY, internalName);

    for (int i = 0, n = sources.length; i < n; ++i) {
      visitor.visitInsn(DUP);
      pushInt(i, visitor);
      visitor.visitFieldInsn(
          GETSTATIC, internalName, sources[i], JoobyTypes.ParamSource.getDescriptor());
      visitor.visitInsn(AASTORE);
    }

    visitor.visitMethodInsn(
        INVOKEINTERFACE,
        paramMethod.getDeclaringType().getInternalName(),
        paramMethod.getName(),
        paramMethod.getDescriptor(),
        true);

    super.accept(writer, controller, handlerInternalName, visitor, parameter, nameGenerator);
  }

  private void pushInt(int value, MethodVisitor visitor) {
    switch (value) {
      case 0:
        visitor.visitInsn(ICONST_0);
        break;
      case 1:
        visitor.visitInsn(ICONST_1);
        break;
      case 2:
        visitor.visitInsn(ICONST_2);
        break;
      case 3:
        visitor.visitInsn(ICONST_3);
        break;
      case 4:
        visitor.visitInsn(ICONST_4);
        break;
      case 5:
        visitor.visitInsn(ICONST_5);
        break;
      default:
        visitor.visitIntInsn(BIPUSH, value);
    }
  }
}
