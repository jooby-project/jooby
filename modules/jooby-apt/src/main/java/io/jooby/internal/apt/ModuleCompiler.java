/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.MvcFactory;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.annotations.Dispatch;
import io.jooby.internal.apt.asm.ArrayWriter;
import io.jooby.internal.apt.asm.RouteAttributesWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
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

public class ModuleCompiler {
  private static final Type OBJ = getType(Object.class);
  private static final Type MVC_FACTORY = getType(MvcFactory.class);

  private final String controllerClass;
  private final String moduleClass;
  private final String moduleInternalName;
  private final String moduleJava;
  private final ProcessingEnvironment processingEnv;
  private final String moduleDescriptorName;
  private final boolean debug;

  public ModuleCompiler(ProcessingEnvironment processingEnv, String controllerClass) {
    this.controllerClass = controllerClass;
    this.moduleClass = this.controllerClass + "$Module";
    this.moduleJava = this.moduleClass + ".java";
    this.moduleInternalName = moduleClass.replace(".", "/");
    this.moduleDescriptorName = "L" + moduleInternalName + ";";
    this.processingEnv = processingEnv;
    this.debug = Boolean.parseBoolean(processingEnv.getOptions().getOrDefault("debug", "false"));
  }

  public String getModuleClass() {
    return moduleClass;
  }

  public byte[] compile(List<HandlerCompiler> handlers) throws Exception {
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    // public class Controller$methodName implements Route.Handler {
    writer.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, moduleInternalName, null,
        OBJ.getInternalName(),
        new String[]{MVC_FACTORY.getInternalName()});
    writer.visitSource(moduleJava, null);

    defaultConstructor(writer);

    supports(writer);

    create(writer);

    install(writer, handlers);

    writer.visitEnd();
    return writer.toByteArray();
  }

  private void defaultConstructor(ClassWriter writer) {
    // Constructor:
    MethodVisitor constructor = writer
        .visitMethod(ACC_PUBLIC, "<init>", "()V", null,
            null);
    constructor.visitCode();
    constructor.visitVarInsn(ALOAD, 0);
    constructor.visitMethodInsn(INVOKESPECIAL, OBJ.getInternalName(), "<init>", "()V", false);
    constructor.visitInsn(RETURN);
    constructor.visitMaxs(0, 0);
    constructor.visitEnd();
  }

  private void create(ClassWriter writer) {
    String lambdaCreate = "makeExtension";
    MethodVisitor methodVisitor = writer
        .visitMethod(ACC_PUBLIC, "create", "(Ljavax/inject/Provider;)Lio/jooby/Extension;", null,
            null);
    methodVisitor.visitParameter("provider", 0);
    methodVisitor.visitCode();
    methodVisitor.visitVarInsn(ALOAD, 1);
    methodVisitor.visitInvokeDynamicInsn("install", "(Ljavax/inject/Provider;)Lio/jooby/Extension;",
        new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false), new Object[]{Type.getType("(Lio/jooby/Jooby;)V"),
            new Handle(Opcodes.H_INVOKESTATIC, moduleInternalName, lambdaCreate,
                "(Ljavax/inject/Provider;Lio/jooby/Jooby;)V", false),
            Type.getType("(Lio/jooby/Jooby;)V")});
    methodVisitor.visitInsn(ARETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();

    makeExtension(writer, lambdaCreate);
  }

  private void makeExtension(ClassWriter writer, String methodName) {
    MethodVisitor methodVisitor = writer
        .visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, methodName,
            "(Ljavax/inject/Provider;Lio/jooby/Jooby;)V", null,
            new String[]{"java/lang/Exception"});
    methodVisitor.visitParameter("provider", Opcodes.ACC_FINAL | ACC_SYNTHETIC);
    methodVisitor.visitParameter("app", ACC_SYNTHETIC);
    methodVisitor.visitCode();
    methodVisitor.visitVarInsn(ALOAD, 1);
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitMethodInsn(INVOKESTATIC, moduleInternalName, "install",
        "(Lio/jooby/Jooby;Ljavax/inject/Provider;)V", false);
    methodVisitor.visitInsn(RETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
  }

  private void install(ClassWriter writer, List<HandlerCompiler> handlers) throws Exception {
    MethodVisitor visitor = writer.visitMethod(ACC_PRIVATE | ACC_STATIC, "install",
        "(Lio/jooby/Jooby;Ljavax/inject/Provider;)V",
        "(Lio/jooby/Jooby;Ljavax/inject/Provider<" + Type.getObjectType(controllerClass.replace(".", "/")) + ">;)V",
        new String[]{"java/lang/Exception"});
    visitor.visitParameter("app", 0);
    visitor.visitParameter("provider", 0);
    visitor.visitCode();

    String[] userAttrFilter = Opts.stringListOpt(processingEnv, Opts.OPT_SKIP_ATTRIBUTE_ANNOTATIONS, "");

    RouteAttributesWriter routeAttributes = new RouteAttributesWriter(processingEnv.getElementUtils(),
        processingEnv.getTypeUtils(), writer, moduleInternalName, visitor, userAttrFilter);

    Map<String, Integer> nameRegistry = new HashMap<>();
    for (HandlerCompiler handler : handlers) {
      visitor.visitVarInsn(ALOAD, 0);
      visitor.visitLdcInsn(handler.getPattern());

      if (handler.isSuspendFunction()) {
        visitor.visitTypeInsn(NEW, "io/jooby/internal/mvc/CoroutineLauncher");
        visitor.visitInsn(DUP);
      }

      visitor.visitVarInsn(ALOAD, 1);
      handler.compile(moduleInternalName, writer, visitor, nameRegistry);
      if (handler.isSuspendFunction()) {
        visitor.visitMethodInsn(INVOKESPECIAL, "io/jooby/internal/mvc/CoroutineLauncher", "<init>",
            "(Lio/jooby/Route$Handler;)V", false);
      }

      visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Jooby", handler.getHttpMethod(),
          "(Ljava/lang/String;Lio/jooby/Route$Handler;)Lio/jooby/Route;", false);
      visitor.visitVarInsn(ASTORE, 2);
      visitor.visitVarInsn(ALOAD, 2);
      /**
       * ******************************************************************************************
       * Return Type:
       * ******************************************************************************************
       */
      setReturnType(visitor, handler);

      /**
       * ******************************************************************************************
       * Consumes and Produces
       * ******************************************************************************************
       */
      setContentType(visitor, "setConsumes", handler.getConsumes());
      setContentType(visitor, "setProduces", handler.getProduces());

      /**
       * ******************************************************************************************
       * Annotations as route attributes
       * ******************************************************************************************
       */
      debug("route attributes %s.%s", handler.getExecutable().getEnclosingElement(),
          handler.getExecutable());
      routeAttributes.process(handler.getExecutable(), this::debug);

      /**
       * ******************************************************************************************
       * Dispatch
       * ******************************************************************************************
       */
      setDispatch(visitor, handler.getExecutable());
    }
    visitor.visitInsn(RETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private void setDispatch(MethodVisitor visitor, ExecutableElement executable)
      throws NoSuchMethodException {
    String executorKey = findAnnotation(executable.getAnnotationMirrors(), Dispatch.class.getName())
        .map(it -> annotationAttribute(it, "value").toString())
        .orElseGet(() ->
            findAnnotation(executable.getEnclosingElement().getAnnotationMirrors(),
                Dispatch.class.getName())
                .map(it -> annotationAttribute(it, "value").toString())
                .orElse(null)
        );

    if (executorKey != null) {
      Method setExecutor = Route.class.getDeclaredMethod("setExecutorKey", String.class);
      visitor.visitVarInsn(ALOAD, 2);
      visitor.visitLdcInsn(executorKey);
      visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(setExecutor.getDeclaringClass()),
          setExecutor.getName(),
          Type.getMethodDescriptor(setExecutor), false);
      visitor.visitInsn(POP);
    }
  }

  private Object annotationAttribute(AnnotationMirror annotationMirror, String method) {
    Map<? extends ExecutableElement, ? extends AnnotationValue> map = processingEnv
        .getElementUtils().getElementValuesWithDefaults(annotationMirror);
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : map.entrySet()) {
      if (entry.getKey().getSimpleName().toString().equals(method)) {
        return entry.getValue().getValue();
      }
    }
    throw new IllegalArgumentException(
        "Missing: " + annotationMirror.getAnnotationType().toString() + "." + method);
  }

  private Optional<? extends AnnotationMirror> findAnnotation(
      List<? extends AnnotationMirror> annotationMirrors, String name) {
    return annotationMirrors.stream()
        .filter(it -> it.getAnnotationType().toString().equals(name))
        .findFirst();
  }

  private void setReturnType(MethodVisitor visitor, HandlerCompiler handler)
      throws NoSuchMethodException {
    TypeDefinition returnType = handler.getReturnType();
    if (handler.isSuspendFunction()) {
      visitor.visitLdcInsn(Type.getType("Lkotlin/coroutines/Continuation;"));
    } else if (returnType.isVoid()) {
      visitor.visitLdcInsn(Type.getType(Context.class));
    } else if (returnType.isPrimitive()) {
      Method wrapper = Primitives.wrapper(returnType);
      visitor.visitFieldInsn(GETSTATIC, Type.getInternalName(wrapper.getDeclaringClass()), "TYPE",
          "Ljava/lang/Class;");
    } else if (returnType.isRawType()) {
      visitor.visitLdcInsn(handler.getReturnType().toJvmType());
    } else {
      visitor.visitLdcInsn(returnType.toJvmType());

      List<TypeDefinition> args = returnType.getArguments();

      ArrayWriter.write(visitor, java.lang.reflect.Type.class.getName(), args, type ->
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
  }

  private void setContentType(MethodVisitor visitor, String methodName, List<String> mediaTypes) {
    if (mediaTypes.size() > 0) {
      visitor.visitVarInsn(ALOAD, 2);
      ArrayWriter.write(visitor, MediaType.class.getName(), mediaTypes, mediaType -> {
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

  private void supports(ClassWriter writer) {
    MethodVisitor visitor = writer
        .visitMethod(ACC_PUBLIC, "supports", "(Ljava/lang/Class;)Z", null, null);
    visitor.visitParameter("type", 0);
    visitor.visitCode();
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitLdcInsn(Type.getObjectType(controllerClass.replace(".", "/")));
    Label l0 = new Label();
    visitor.visitJumpInsn(IF_ACMPNE, l0);
    visitor.visitInsn(Opcodes.ICONST_1);
    Label l1 = new Label();
    visitor.visitJumpInsn(Opcodes.GOTO, l1);
    visitor.visitLabel(l0);
    visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    visitor.visitInsn(Opcodes.ICONST_0);
    visitor.visitLabel(l1);
    visitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
    visitor.visitInsn(Opcodes.IRETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private void debug(String format, Object... args) {
    if (debug) {
      System.out.printf(format + "\n", args);
    }
  }
}
