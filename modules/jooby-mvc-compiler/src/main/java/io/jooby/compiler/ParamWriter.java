package io.jooby.compiler;

import io.jooby.Context;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Type.getType;

interface ParamWriter {
  Type CTX = getType(Context.class);

  void accept(ClassWriter writer, MethodVisitor visitor, ParamDefinition parameter) throws Exception;
}
