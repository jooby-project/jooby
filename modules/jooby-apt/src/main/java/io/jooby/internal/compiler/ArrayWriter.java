/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.compiler;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.function.Consumer;

public class ArrayWriter {
  public static <T> void write(MethodVisitor visitor, Class componentType, List<T> items, Consumer<T> foreach) {
    visitor.visitInsn(Opcodes.ICONST_0 + items.size());
    visitor.visitTypeInsn(Opcodes.ANEWARRAY, componentType.getName().replace(".", "/"));
    for (int i = 0; i < items.size(); i++) {
      visitor.visitInsn(Opcodes.DUP);
      visitor.visitInsn(Opcodes.ICONST_0 + i);
      foreach.accept(items.get(i));
      visitor.visitInsn(Opcodes.AASTORE);
    }
  }
}
