/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import io.jooby.Reified;
import io.jooby.internal.apt.ParamDefinition;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Type.getMethodDescriptor;

public abstract class ValueWriter implements ParamWriter {
  @Override
  public void accept(ClassWriter writer, org.objectweb.asm.Type controller,
      String handlerInternalName, MethodVisitor visitor,
      ParamDefinition parameter, Map<String, Integer> registry) throws Exception {
    Method convertMethod = parameter.getMethod();
    // to(Class)
    boolean toClass = is(convertMethod, 0, Class.class);
    boolean toReified = is(convertMethod, 0, Type.class);
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

        ArrayWriter.write(visitor, Type.class.getName(), parameter.getType().getArguments(), type ->
            visitor.visitLdcInsn(type.toJvmType())
        );
        reified = Reified.class.getMethod("getParameterized", Type.class, Type[].class);
      }
      Method getType = Reified.class.getDeclaredMethod("getType");
      visitor.visitMethodInsn(INVOKESTATIC, "io/jooby/Reified", reified.getName(),
          getMethodDescriptor(reified), false);
      visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Reified", getType.getName(),
          getMethodDescriptor(getType), false);
    }

    visitor.visitMethodInsn(INVOKEINTERFACE,
        org.objectweb.asm.Type.getInternalName(convertMethod.getDeclaringClass()),
        convertMethod.getName(),
        getMethodDescriptor(convertMethod), true);

    if (toClass || toReified) {
      visitor.visitTypeInsn(CHECKCAST, parameter.getType().toJvmType().getInternalName());
      if (!parameter.isNullable()) {
        visitor.visitVarInsn(ASTORE, 3);
        visitor.visitLdcInsn(parameter.getHttpName());
        visitor.visitVarInsn(ALOAD, 3);
        visitor.visitMethodInsn(INVOKESTATIC, "io/jooby/exception/MissingValueException", "requireNonNull", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", false);
        visitor.visitTypeInsn(CHECKCAST, parameter.getType().toJvmType().getInternalName());
      }
    }
  }

  private boolean is(Method method, int index, Class type) {
    Class<?>[] types = method.getParameterTypes();
    return index < types.length && types[index].equals(type);
  }
}
