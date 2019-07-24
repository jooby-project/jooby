package io.jooby.internal.compiler;

import io.jooby.Reified;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Type.getMethodDescriptor;

abstract class ValueWriter implements ParamWriter {
  @Override public void accept(ClassWriter writer, String handlerInternalName, MethodVisitor visitor, ParamDefinition parameter)
      throws Exception {
    Method convertMethod = parameter.getMethod();
    // to(Class)
    boolean toClass = is(convertMethod, 0, Class.class);
    boolean toReified = is(convertMethod, 0, Reified.class);
    // toOptional(Class) or toList(Class) or toSet(Class)
    if (toClass) {
      visitor.visitLdcInsn(parameter.getType().isParameterizedType()
          ? parameter.getType().getArguments().get(0).toJvmType()
          : parameter.getType().toJvmType()
      );
    } else if (toReified) {
      Method reified;
      if (parameter.is(Map.class)) {
        visitor.visitLdcInsn(parameter.getType().getArguments().get(0).toJvmType());
        visitor.visitLdcInsn(parameter.getType().getArguments().get(1).toJvmType());
        reified = Reified.class.getMethod("map", Type.class, Type.class);
      } else {
        visitor.visitLdcInsn(parameter.getType().toJvmType());
        List<TypeDefinition> args = parameter.getType().getArguments();
        visitor.visitInsn(Opcodes.ICONST_0 + args.size());
        visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/reflect/Type");
        for (int i = 0; i < args.size(); i++) {
          visitor.visitInsn(Opcodes.DUP);
          visitor.visitInsn(Opcodes.ICONST_0 + i);
          visitor.visitLdcInsn(args.get(i).toJvmType());
          visitor.visitInsn(Opcodes.AASTORE);
        }
        reified = Reified.class.getMethod("getParameterized", Type.class, Type[].class);
      }

      visitor.visitMethodInsn(INVOKESTATIC, "io/jooby/Reified", reified.getName(),
          getMethodDescriptor(reified), false);
    }

    visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Value", convertMethod.getName(),
        getMethodDescriptor(convertMethod), true);

    if (toClass || toReified) {
      visitor.visitTypeInsn(CHECKCAST, parameter.getType().toJvmType().getInternalName());
    }
  }

  private boolean is(Method method, int index, Class type) {
    Class<?>[] types = method.getParameterTypes();
    return index < types.length && types[index].equals(type);
  }
}
