/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.apt.Annotations;
import io.jooby.internal.apt.asm.ParamWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Provider;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

public class HandlerCompiler {

  private static final Type OBJ = getType(Object.class);
  private static final Type STATUS_CODE = getType(StatusCode.class);

  private static final Type PROVIDER = getType(Provider.class);
  private static final String PROVIDER_DESCRIPTOR = getMethodDescriptor(OBJ);

  private static final Type CTX = getType(Context.class);

  private TypeDefinition owner;
  private ExecutableElement executable;
  private ProcessingEnvironment environment;
  private String httpMethod;
  private String pattern;
  private Types typeUtils;
  private TypeMirror annotation;

  public HandlerCompiler(ProcessingEnvironment environment, TypeElement owner,
      ExecutableElement executable,
      TypeElement httpMethod, String pattern) {
    this.httpMethod = httpMethod.getSimpleName().toString().toLowerCase();
    this.annotation = httpMethod.asType();
    this.pattern = Router.leadingSlash(pattern);
    this.environment = environment;
    this.executable = executable;
    this.typeUtils = environment.getTypeUtils();
    this.owner = new TypeDefinition(typeUtils, owner.asType());
  }

  public ExecutableElement getExecutable() {
    return executable;
  }

  public String getPattern() {
    return pattern;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public TypeDefinition getReturnType() {
    return new TypeDefinition(typeUtils, executable.getReturnType());
  }

  public List<String> getConsumes() {
    return mediaType(executable, annotation, "consumes", Annotations.CONSUMES_PARAMS);
  }

  public List<String> getProduces() {
    return mediaType(executable, annotation, "produces", Annotations.PRODUCES_PARAMS);
  }

  public void compile(String internalName, ClassWriter writer,
      MethodVisitor methodVisitor, Map<String, Integer> nameRegistry)
      throws Exception {
    String key =
        httpMethod + camelCase(executable.getSimpleName().toString()) + arguments(executable);
    int c = nameRegistry.computeIfAbsent(key, k -> 0);
    String methodName;
    if (c > 0) {
      methodName = key + "$" + c;
    } else {
      methodName = key;
    }
    nameRegistry.put(key, c + 1);

    methodVisitor
        .visitInvokeDynamicInsn("apply", "(Ljavax/inject/Provider;)Lio/jooby/Route$Handler;",
            new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false), new Object[]{Type.getType("(Lio/jooby/Context;)Ljava/lang/Object;"),
                new Handle(Opcodes.H_INVOKESTATIC, internalName, methodName,
                    "(Ljavax/inject/Provider;Lio/jooby/Context;)Ljava/lang/Object;", false),
                Type.getType("(Lio/jooby/Context;)Ljava/lang/Object;")});

    /** Apply implementation: */
    apply(writer, internalName, methodName, nameRegistry);
  }

  private String camelCase(String name) {
    if (name.length() > 1) {
      return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    return name.toUpperCase();
  }

  private String arguments(ExecutableElement executable) {
    StringBuilder buff = new StringBuilder();
    for (VariableElement var : executable.getParameters()) {
      buff.append("$").append(var.getSimpleName());
    }
    return buff.toString();
  }

  private void apply(ClassWriter writer, String moduleInternalName, String lambdaName,
      Map<String, Integer> registry)
      throws Exception {
    Type owner = getController().toJvmType();
    String methodName = executable.getSimpleName().toString();
    String methodDescriptor = methodDescriptor();
    MethodVisitor apply = writer
        .visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, lambdaName,
            "(Ljavax/inject/Provider;Lio/jooby/Context;)Ljava/lang/Object;", null,
            new String[]{"java/lang/Exception"});
    apply.visitParameter("provider", ACC_FINAL | ACC_SYNTHETIC);
    apply.visitParameter("ctx", ACC_SYNTHETIC);

    apply.visitCode();

    Label sourceStart = new Label();
    apply.visitLabel(sourceStart);

    /**
     * provider.get()
     */
    apply.visitVarInsn(ALOAD, 0);
    apply.visitMethodInsn(INVOKEINTERFACE, PROVIDER.getInternalName(), "get", PROVIDER_DESCRIPTOR,
        true);
    apply.visitTypeInsn(CHECKCAST, owner.getInternalName());
    apply.visitVarInsn(ASTORE, 2);
    apply.visitVarInsn(ALOAD, 2);

    /** Arguments. */
    processArguments(writer, apply, moduleInternalName, registry);

    setDefaultResponseType(apply);

    /** Invoke. */
    apply.visitMethodInsn(INVOKEVIRTUAL, owner.getInternalName(), methodName, methodDescriptor,
        false);

    processReturnType(apply);

    apply.visitEnd();
  }

  public boolean isSuspendFunction() {
    List<? extends VariableElement> parameters = executable.getParameters();
    if (parameters.isEmpty()) {
      return false;
    }
    VariableElement last = parameters.get(parameters.size() - 1);
    return isSuspendFunction(last);
  }

  private boolean isSuspendFunction(VariableElement parameter) {
    String type = ParamDefinition.create(environment, parameter).getType().getRawType().toString();
    return type.equals("kotlin.coroutines.Continuation");
  }

  private void processArguments(ClassWriter classWriter, MethodVisitor visitor,
      String moduleInternalName, Map<String, Integer> registry) throws Exception {
    for (VariableElement var : executable.getParameters()) {
      if (isSuspendFunction(var)) {
        visitor.visitVarInsn(ALOAD, 1);
        visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Context", "getAttributes",
            "()Ljava/util/Map;", true);
        visitor.visitLdcInsn("___continuation");
        visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "remove",
            "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        visitor.visitTypeInsn(CHECKCAST, "kotlin/coroutines/Continuation");
      } else {
        visitor.visitVarInsn(ALOAD, 1);
        ParamDefinition param = ParamDefinition.create(environment, var);
        ParamWriter writer = param.newWriter();
        writer.accept(classWriter, moduleInternalName, visitor, param, registry);
      }
    }
  }

  private void setDefaultResponseType(MethodVisitor visitor) throws Exception {
    TypeKind kind = executable.getReturnType().getKind();
    if (kind == TypeKind.VOID && getHttpMethod().equalsIgnoreCase(Router.DELETE)) {
      visitor.visitVarInsn(ALOAD, 1);
      visitor
          .visitFieldInsn(GETSTATIC, STATUS_CODE.getInternalName(), "NO_CONTENT",
              STATUS_CODE.getDescriptor());
      Method setResponseCode = Context.class.getDeclaredMethod("setResponseCode", StatusCode.class);
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), setResponseCode.getName(),
          getMethodDescriptor(setResponseCode), true);
      visitor.visitInsn(POP);
    }
  }

  private void processReturnType(MethodVisitor visitor) throws Exception {
    TypeKind kind = executable.getReturnType().getKind();
    if (kind == TypeKind.VOID) {
      visitor.visitVarInsn(ALOAD, 1);
      Method isResponseStarted = Context.class.getDeclaredMethod("isResponseStarted");
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), isResponseStarted.getName(),
          getMethodDescriptor(isResponseStarted), true);
      Label label0 = new Label();
      visitor.visitJumpInsn(IFEQ, label0);
      visitor.visitVarInsn(ALOAD, 1);
      visitor.visitInsn(ARETURN);
      visitor.visitLabel(label0);
      visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

      visitor.visitVarInsn(ALOAD, 1);
      visitor.visitVarInsn(ALOAD, 1);
      Method getResponseCode = Context.class.getDeclaredMethod("getResponseCode");
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), getResponseCode.getName(),
          getMethodDescriptor(getResponseCode), true);
      Method sendStatusCode = Context.class.getDeclaredMethod("send", StatusCode.class);
      visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), sendStatusCode.getName(),
          getMethodDescriptor(sendStatusCode), true);
    } else {
      Method wrapper = Primitives.wrapper(kind);
      if (wrapper == null) {
        TypeDefinition returnType = getReturnType();
        if (returnType.is(StatusCode.class)) {
          visitor.visitVarInsn(ASTORE, 2);
          visitor.visitVarInsn(ALOAD, 1);
          visitor.visitVarInsn(ALOAD, 2);
          Method send = Context.class.getDeclaredMethod("send", StatusCode.class);
          visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), send.getName(),
              getMethodDescriptor(send), true);
        }
      } else {
        // Primitive wrapper
        visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(wrapper.getDeclaringClass()),
            wrapper.getName(), getMethodDescriptor(wrapper), false);
      }
    }
    visitor.visitInsn(ARETURN);

    visitor.visitMaxs(0, 0);
  }

  public TypeDefinition getController() {
    return owner;
  }

  private String methodDescriptor() {
    Types typeUtils = environment.getTypeUtils();
    Type returnType = new TypeDefinition(typeUtils, executable.getReturnType()).toJvmType();
    Type[] arguments = executable.getParameters().stream()
        .map(var -> new TypeDefinition(typeUtils, var.asType()).toJvmType())
        .toArray(Type[]::new);
    return Type.getMethodDescriptor(returnType, arguments);
  }

  private List<String> mediaType(ExecutableElement element, TypeMirror annotation, String property,
      Set<String> types) {
    List<String> result = element.getAnnotationMirrors().stream()
        .filter(it -> it.getAnnotationType().equals(annotation))
        .findFirst()
        .map(it -> Annotations.attribute(it, property))
        .orElse(Collections.emptyList());
    if (result.size() == 0) {
      return mediaType(element, types);
    }
    return result;
  }

  private List<String> mediaType(Element element, Set<String> types) {
    return element.getAnnotationMirrors().stream()
        .filter(it -> types.contains(it.getAnnotationType().toString()))
        .findFirst()
        .map(it -> Annotations.attribute(it, "value"))
        .orElseGet(() -> {
          if (element instanceof ExecutableElement) {
            return mediaType(element.getEnclosingElement(), types);
          }
          return Collections.emptyList();
        });
  }
}
