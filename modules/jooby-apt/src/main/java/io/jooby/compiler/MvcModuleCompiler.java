/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.compiler;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.MvcModule;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.internal.compiler.ArrayWriter;
import io.jooby.internal.compiler.ConstructorWriter;
import io.jooby.internal.compiler.TypeDefinition;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

public class MvcModuleCompiler {
  private static final Type OBJ = getType(Object.class);
  private static final Type MVC_EXTENSION = getType(Extension.class);

  private final String controllerClass;
  private final String moduleClass;
  private final String moduleInternalName;
  private final String moduleJava;

  public MvcModuleCompiler(String controllerClass) {
    this.controllerClass = controllerClass;
    this.moduleClass = this.controllerClass + "$Module";
    this.moduleJava = this.moduleClass + ".java";
    this.moduleInternalName = moduleClass.replace(".", "/");
  }

  public String getModuleClass() {
    return moduleClass;
  }

  public byte[] compile(List<Map.Entry<String, MvcHandlerCompiler>> handlers) throws Exception {
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    // public class Controller$methodName implements Route.Handler {
    writer.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, moduleInternalName, null,
        OBJ.getInternalName(),
        new String[]{MVC_EXTENSION.getInternalName()});
    writer.visitSource(moduleJava, null);

    new ConstructorWriter()
        .build(moduleClass, writer);

    install(writer, handlers.stream().map(Map.Entry::getValue).collect(Collectors.toList()));

    writer.visitEnd();
    return writer.toByteArray();
  }

  private void install(ClassWriter writer,
      List<MvcHandlerCompiler> handlers) throws Exception {
    Method install = Extension.class.getDeclaredMethod("install", Jooby.class);
    MethodVisitor visitor = writer
        .visitMethod(ACC_PUBLIC, install.getName(), Type.getMethodDescriptor(install), null, null);
    visitor.visitParameter("app", 0);
    visitor.visitCode();
    Label sourceStart = new Label();
    visitor.visitLabel(sourceStart);

    for (MvcHandlerCompiler handler : handlers) {
      visitor.visitVarInsn(ALOAD, 1);
      visitor.visitLdcInsn(handler.getPattern());
      visitor.visitTypeInsn(NEW, handler.getGeneratedInternalClass());
      visitor.visitInsn(DUP);
      visitor.visitVarInsn(ALOAD, 0);
      visitor.visitFieldInsn(GETFIELD, moduleInternalName, "provider",
          "Ljavax/inject/Provider;");
      visitor.visitMethodInsn(INVOKESPECIAL, handler.getGeneratedInternalClass(), "<init>",
          "(Ljavax/inject/Provider;)V", false);
      visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Jooby", handler.getHttpMethod(),
          "(Ljava/lang/String;Lio/jooby/Route$Handler;)Lio/jooby/Route;", false);
      visitor.visitVarInsn(ASTORE, 2);
      visitor.visitVarInsn(ALOAD, 2);
      /**
       * ******************************************************************************************
       * Return Type:
       * ******************************************************************************************
       */
      TypeDefinition returnType = handler.getReturnType();
      if (returnType.isRawType()) {
        visitor.visitLdcInsn(handler.getReturnType().toJvmType());
      } else {
        visitor.visitLdcInsn(returnType.toJvmType());

        List<TypeDefinition> args = returnType.getArguments();

        ArrayWriter.write(visitor, java.lang.reflect.Type.class, args, type ->
            visitor.visitLdcInsn(type.toJvmType())
        );

        Method reified = Reified.class.getMethod("getParameterized", java.lang.reflect.Type.class,
            java.lang.reflect.Type[].class);
        visitor.visitMethodInsn(INVOKESTATIC, "io/jooby/Reified", reified.getName(),
            getMethodDescriptor(reified), false);
        Method reifiedToType = Reified.class.getDeclaredMethod("getType");
        visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Reified", reifiedToType.getName(),
            getMethodDescriptor(reifiedToType), false);
      }
      Method setReturnType = Route.class
          .getDeclaredMethod("setReturnType", java.lang.reflect.Type.class);
      visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Route", setReturnType.getName(),
          getMethodDescriptor(setReturnType), false);
      visitor.visitInsn(POP);
      /**
       * ******************************************************************************************
       * Consumes and Produces
       * ******************************************************************************************
       */
      setContentType(visitor, "setConsumes", handler.getConsumes());
      setContentType(visitor, "setProduces", handler.getProduces());
    }
    visitor.visitInsn(RETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private void setContentType(MethodVisitor visitor, String methodName, List<String> mediaTypes) {
    if (mediaTypes.size() > 0) {
      visitor.visitVarInsn(ALOAD, 2);
      ArrayWriter.write(visitor, MediaType.class, mediaTypes, mediaType -> {
        visitor.visitLdcInsn(mediaType);
        visitor.visitMethodInsn(INVOKESTATIC, "io/jooby/MediaType", "valueOf",
            "(Ljava/lang/String;)Lio/jooby/MediaType;", false);
      });
      visitor.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList",
          "([Ljava/lang/Object;)Ljava/util/List;", false);
      visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Route", methodName,
          "(Ljava/util/Collection;)Lio/jooby/Route;", false);
      visitor.visitInsn(POP);
    }
  }
}
