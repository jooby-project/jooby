/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.compiler;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.apt.Annotations;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Provider;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

public class HandlerCompiler {

  private static final Type OBJ = getType(Object.class);
  private static final Type HANDLER = getType(Route.Handler.class);
  private static final Type STATUS_CODE = getType(StatusCode.class);

  private static final Type PROVIDER = getType(Provider.class);
  private static final String PROVIDER_VAR = "provider";
  private static final String PROVIDER_DESCRIPTOR = getMethodDescriptor(OBJ);

  private static final Type CTX = getType(Context.class);

  private static final String APPLY_DESCRIPTOR = getMethodDescriptor(OBJ, CTX);
  private static final String[] APPLY_THROWS = new String[]{getInternalName(Exception.class)};

  private final TypeDefinition owner;
  private final ExecutableElement executable;
  private final ProcessingEnvironment environment;
  private final String httpMethod;
  private final String pattern;
  private final Types typeUtils;
  private final TypeMirror annotation;

  public HandlerCompiler(ProcessingEnvironment environment, ExecutableElement executable,
      TypeElement httpMethod, String pattern) {
    this.httpMethod = httpMethod.getSimpleName().toString().toLowerCase();
    this.annotation = httpMethod.asType();
    this.pattern = Router.normalizePath(pattern, false, true);
    this.environment = environment;
    this.executable = executable;
    this.typeUtils = environment.getTypeUtils();
    this.owner = new TypeDefinition(typeUtils, executable.getEnclosingElement().asType());
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

  public byte[] compile() throws Exception {
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    // public class Controller$methodName implements Route.Handler {
    writer.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, getGeneratedInternalClass(), null,
        OBJ.getInternalName(),
        new String[]{HANDLER.getInternalName()});

    writer.visitSource(getController().getSimpleName() + ".java", null);

    writer.visitInnerClass(HANDLER.getInternalName(), getInternalName(Route.class),
        Route.Handler.class.getSimpleName(),
        ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE);

    // Constructor(Provider<Controller> provider)
    new ConstructorWriter()
        .build(getGeneratedClass(), writer);

    /** Apply implementation: */
    apply(writer);

    writer.visitEnd();
    return writer.toByteArray();
  }

  private void apply(ClassWriter writer) throws Exception {
    Type owner = getController().toJvmType();
    String methodName = executable.getSimpleName().toString();
    String methodDescriptor = methodDescriptor();
    MethodVisitor apply = writer
        .visitMethod(ACC_PUBLIC, "apply", APPLY_DESCRIPTOR, null, APPLY_THROWS);
    apply.visitParameter("ctx", 0);
    apply.visitCode();

    Label sourceStart = new Label();
    apply.visitLabel(sourceStart);

    /**
     * provider.get()
     */
    apply.visitVarInsn(ALOAD, 0);
    apply.visitFieldInsn(GETFIELD, getGeneratedInternalClass(), PROVIDER_VAR,
        PROVIDER.getDescriptor());
    apply.visitMethodInsn(INVOKEINTERFACE, PROVIDER.getInternalName(), "get", PROVIDER_DESCRIPTOR,
        true);
    apply.visitTypeInsn(CHECKCAST, owner.getInternalName());

    /** Arguments. */
    processArguments(writer, apply);

    /** Invoke. */
    apply.visitMethodInsn(INVOKEVIRTUAL, owner.getInternalName(), methodName, methodDescriptor,
        false);

    processReturnType(apply);

    apply.visitEnd();
  }

  private void processArguments(ClassWriter classWriter, MethodVisitor visitor) throws Exception {
    for (VariableElement var : executable.getParameters()) {
      visitor.visitVarInsn(ALOAD, 1);
      ParamDefinition param = ParamDefinition.create(environment, var);
      ParamWriter writer = param.newWriter();
      writer.accept(classWriter, getGeneratedInternalClass(), visitor, param);
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
      visitor
          .visitFieldInsn(GETSTATIC, STATUS_CODE.getInternalName(), "NO_CONTENT",
              STATUS_CODE.getDescriptor());
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

  @Override public String toString() {
    try {
      ClassReader reader = new ClassReader(compile());
      ByteArrayOutputStream buff = new ByteArrayOutputStream();
      Printer printer = new ASMifier();
      TraceClassVisitor traceClassVisitor =
          new TraceClassVisitor(null, printer, new PrintWriter(buff));

      reader.accept(traceClassVisitor, ClassReader.SKIP_DEBUG);

      return new String(buff.toByteArray());
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public String getGeneratedClass() {
    StringBuilder name = new StringBuilder(getController().getName());
    name.append("$");
    name.append(httpMethod.toUpperCase());

    for (int i = 0; i < pattern.length(); i++) {
      char ch = pattern.charAt(i);
      if (Character.isJavaIdentifierPart(ch)) {
        name.append(ch);
      } else if (ch == '/') {
        name.append('$');
      } else {
        name.append('_');
      }
    }
    for (VariableElement var : executable.getParameters()) {
      name.append("$").append(var.getSimpleName());
    }
    return name.toString();
  }

  public String getGeneratedInternalClass() {
    return getGeneratedClass().replace(".", "/");
  }

  public TypeDefinition getController() {
    return owner;
  }

  public String getKey() {
    return httpMethod.toUpperCase() + pattern;
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
