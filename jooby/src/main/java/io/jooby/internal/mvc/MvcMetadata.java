/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import io.jooby.internal.asm.ClassSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MvcMetadata {

  private Map<String, MvcMethod> metadata = new HashMap<>();

  private ClassSource source;

  public MvcMetadata(ClassSource source) {
    this.source = source;
  }

  public void parse(Class router) {
    ClassReader reader = new ClassReader(source.byteCode(router));
    reader.accept(visitor(metadata), 0);
  }

  public MvcMethod create(Method method) {
    String key =
        Type.getType(method.getDeclaringClass()).getInternalName() + "." + method.getName() + Type
            .getMethodDescriptor(method);
    return metadata.get(key).copy();
  }

  public void destroy() {
    metadata.values().forEach(MvcMethod::destroy);
    metadata.clear();
    metadata = null;
  }

  private static ClassVisitor visitor(final Map<String, MvcMethod> md) {
    return new ClassVisitor(Opcodes.ASM5) {

      private String classname;

      private String source;

      @Override
      public void visit(int version, int access, String name, String signature, String superName,
          String[] interfaces) {
        this.classname = name;
      }

      @Override public void visitSource(String source, String debug) {
        this.source = source;
      }

      @Override
      public MethodVisitor visitMethod(final int access, final String name,
          final String desc, final String signature, final String[] exceptions) {
        boolean isPublic = ((access & Opcodes.ACC_PUBLIC) > 0) ? true : false;
        boolean isStatic = ((access & Opcodes.ACC_STATIC) > 0) ? true : false;
        if (!isPublic || isStatic) {
          // ignore
          return null;
        }
        final String key = classname + "." + name + desc;
        Type[] args = Type.getArgumentTypes(desc);
        MvcMethod e = new MvcMethod();
        e.source = source;
        md.put(key, e);

        int minIdx = ((access & Opcodes.ACC_STATIC) > 0) ? 0 : 1;
        int maxIdx = Arrays.stream(args).mapToInt(Type::getSize).sum();

        return new MethodVisitor(Opcodes.ASM5) {

          private boolean skipLocalTable = false;

          @Override
          public void visitParameter(final String name, final int access) {
            skipLocalTable = true;
            // save current parameter
            e.parameter(name);
          }

          @Override
          public void visitLineNumber(final int line, final Label start) {
            // save line number
            if (e.line <= 0) {
              e.line = line;
            }
          }

          @Override
          public void visitLocalVariable(final String name, final String desc,
              final String signature, final Label start, final Label end, final int index) {
            if (!skipLocalTable) {
              if (index >= minIdx && index <= maxIdx) {
                // save current parameter
                e.parameter(name);
              }
            }
          }
        };
      }
    };
  }
}
