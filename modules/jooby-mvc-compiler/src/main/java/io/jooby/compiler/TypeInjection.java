package io.jooby.compiler;

import io.jooby.Context;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class TypeInjection implements ParamWriter {
  @Override public void accept(ClassWriter writer, MethodVisitor visitor, ParamDefinition parameter)
      throws Exception {
    if (!parameter.is(Context.class)) {
      Method method = ParamStrategy.forTypeInjection(parameter).valueObject(parameter);
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), method.getName(),
          Type.getMethodDescriptor(method), true);
    }
    if (parameter.isOptional()) {
      visitor.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "ofNullable",
          "(Ljava/lang/Object;)Ljava/util/Optional;", false);
    }
  }
}
