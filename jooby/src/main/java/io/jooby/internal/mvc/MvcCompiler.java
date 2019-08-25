/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import io.jooby.Context;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.ProvisioningException;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Value;
import io.jooby.internal.ValueInjector;
import io.jooby.internal.reflect.$Types;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.inject.Provider;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.F_SAME1;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

public class MvcCompiler {

  private static final Type OBJ = getType(Object.class);

  private static final Type PROVIDER = getType(Provider.class);
  private static final String PROVIDER_VAR = "provider";
  private static final String PROVIDER_DESCRIPTOR = getMethodDescriptor(OBJ);

  private static final Type STRING = getType(String.class);

  private static final Type VALUE = getType(Value.class);
  private static final String VALUE_NAME = VALUE.getInternalName();

  private static final Type CTX = getType(Context.class);
  private static final String CTX_INTERNAL = CTX.getInternalName();

  private static final Type INT = getType(Integer.class);
  private static final String INT_NAME = INT.getInternalName();
  private static final String IVALUEOF = getMethodDescriptor(INT, Type.INT_TYPE);

  private static final Type LONG = getType(Long.class);
  private static final String LONG_NAME = LONG.getInternalName();
  private static final String LVALUEOF = getMethodDescriptor(LONG, Type.LONG_TYPE);

  private static final Type FLOAT = getType(Float.class);
  private static final String FLOAT_NAME = FLOAT.getInternalName();
  private static final String FVALUEOF = getMethodDescriptor(FLOAT, FLOAT_TYPE);

  private static final Type DOUBLE = getType(Double.class);
  private static final String DOUBLE_NAME = DOUBLE.getInternalName();
  private static final String DVALUEOF = getMethodDescriptor(DOUBLE, DOUBLE_TYPE);

  private static final Type BOOL = getType(Boolean.class);
  private static final String BOOL_NAME = BOOL.getInternalName();
  private static final String BVALUEOF = getMethodDescriptor(BOOL, BOOLEAN_TYPE);

  private static final String APPLY_DESCRIPTOR = getMethodDescriptor(OBJ, CTX);
  private static final String[] APPLY_THROWS = new String[]{getInternalName(Exception.class)};

  private static final String PROVISIONING = getInternalName(ProvisioningException.class);

  private static final String[] HANDLER_IMPLEMENTS = new String[]{
      getInternalName(MvcHandler.class)};

  private static final int ALOAD_CTX = 1;

  private static final int ALOAD_OWNER = 2;

  private static final BiConsumer<String, String> NOOP = (varname, type) -> {
  };

  private static final Set<Class> NATIVE = new LinkedHashSet<>(asList(
      Context.class,
      QueryString.class,
      Formdata.class,
      Multipart.class,
      FlashMap.class
  ));

  public static Class<? extends MvcHandler> compileClass(MvcMethod method)
      throws ClassNotFoundException {
    byte[] bytes = compile(method);
    String handlername = method.getHandlerName();
    ClassLoader parent = method.getClass().getClassLoader();
    return (Class<? extends MvcHandler>) new ClassLoader(parent) {
      @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (handlername.equals(name)) {
          return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
      }
    }.loadClass(handlername);
  }

  public static byte[] compile(MvcMethod metadata) {
    Method method = metadata.getMethod();
    String descriptor = getMethodDescriptor(method);
    Type owner = getType(method.getDeclaringClass());
    String internalName = metadata.getHandlerName().replace(".", "/");
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    writer.visit(V1_8, ACC_PUBLIC | ACC_SUPER, internalName, null, OBJ.getInternalName(),
        HANDLER_IMPLEMENTS);

    writer.visitSource(metadata.getSource(), null);

    writer.visitInnerClass(getInternalName(Route.Handler.class),
        getInternalName(Route.class),
        Route.Handler.class.getSimpleName(),
        ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE);

    // Supplier<T> provider;
    String provider = providerOf(owner);

    writer.visitField(ACC_PRIVATE, PROVIDER_VAR, PROVIDER.getDescriptor(), provider, null)
        .visitEnd();

    // Constructor:
    MethodVisitor constructor = writer
        .visitMethod(ACC_PUBLIC, "<init>",
            getMethodDescriptor(Type.VOID_TYPE, PROVIDER),
            getMethodDescriptor(Type.VOID_TYPE, getType(provider)),
            null);
    constructor.visitParameter(PROVIDER_VAR, 0);
    constructor.visitCode();
    constructor.visitVarInsn(ALOAD, 0);
    constructor.visitMethodInsn(INVOKESPECIAL, OBJ.getInternalName(), "<init>", "()V", false);
    constructor.visitVarInsn(ALOAD, 0);
    constructor.visitVarInsn(ALOAD, 1);
    constructor.visitFieldInsn(PUTFIELD, internalName, PROVIDER_VAR, PROVIDER.getDescriptor());
    constructor.visitInsn(RETURN);
    constructor.visitMaxs(2, 2);
    constructor.visitEnd();

    /** Param methods: */
    Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      tryParam(metadata, writer, parameters[i], i);
    }

    /** Arguments: */
    args(internalName, metadata, writer.visitMethod(ACC_PUBLIC | Opcodes.ACC_FINAL, "arguments",
        "(Lio/jooby/Context;)[Ljava/lang/Object;", null, null),
        parameters);

    /** Apply implementation: */
    MethodVisitor apply = writer
        .visitMethod(ACC_PUBLIC, "apply", APPLY_DESCRIPTOR, null, APPLY_THROWS);
    apply.visitParameter("ctx", 0);
    apply.visitCode();

    Label sourceStart = new Label();
    apply.visitLabel(sourceStart);
    apply.visitLineNumber(metadata.getLine(), sourceStart);

    /** load mvc instance: */
    apply.visitVarInsn(ALOAD, 0);
    apply.visitFieldInsn(GETFIELD, internalName, PROVIDER_VAR, PROVIDER.getDescriptor());
    apply.visitMethodInsn(INVOKEINTERFACE, PROVIDER.getInternalName(), "get", PROVIDER_DESCRIPTOR,
        true);
    apply.visitTypeInsn(CHECKCAST, getInternalName(metadata.method.getDeclaringClass()));

    apply.visitVarInsn(ASTORE, ALOAD_OWNER);
    apply.visitVarInsn(ALOAD, ALOAD_OWNER);

    /** call mvc method arguments: */
    for (int i = 0; i < parameters.length; i++) {
      invokeTryParam(internalName, apply, parameters[i], metadata.getParameterName(i));
    }

    /** call mvc method: */
    if (method.getDeclaringClass().isInterface()) {
      apply
          .visitMethodInsn(INVOKEINTERFACE, owner.getInternalName(), method.getName(), descriptor,
              true);
    } else {
      apply.visitMethodInsn(INVOKEVIRTUAL, owner.getInternalName(), method.getName(), descriptor,
          false);
    }

    /** returns: */
    Class<?> returnType = method.getReturnType();
    if (returnType == void.class) {
      apply.visitVarInsn(ALOAD, ALOAD_CTX);
      apply.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "isResponseStarted", "()Z", true);
      Label label0 = new Label();
      apply.visitJumpInsn(IFEQ, label0);
      apply.visitVarInsn(ALOAD, 1);
      apply.visitInsn(ARETURN);
      apply.visitLabel(label0);
      apply.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

      apply.visitVarInsn(ALOAD, ALOAD_CTX);
      apply.visitFieldInsn(GETSTATIC, "io/jooby/StatusCode", "NO_CONTENT",
          "Lio/jooby/StatusCode;");
      apply.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "send",
          "(Lio/jooby/StatusCode;)Lio/jooby/Context;", true);
      apply.visitInsn(Opcodes.POP);
      apply.visitVarInsn(ALOAD, 1);
    } else if (returnType.isPrimitive()) {
      primitiveToWrapper(apply, returnType);
    }

    // Writer was created with COMPUTE_MAXS
    apply.visitInsn(ARETURN);
    apply.visitMaxs(0, 0);
    apply.visitEnd();
    writer.visitEnd();
    return writer.toByteArray();
  }

  private static void primitiveToWrapper(MethodVisitor visitor, Class type) {
    if (type == int.class) {
      visitor.visitMethodInsn(INVOKESTATIC, INT_NAME, "valueOf", IVALUEOF, false);
    } else if (type == long.class) {
      visitor.visitMethodInsn(INVOKESTATIC, LONG_NAME, "valueOf", LVALUEOF, false);
    } else if (type == float.class) {
      visitor.visitMethodInsn(INVOKESTATIC, FLOAT_NAME, "valueOf", FVALUEOF, false);
    } else if (type == double.class) {
      visitor.visitMethodInsn(INVOKESTATIC, DOUBLE_NAME, "valueOf", DVALUEOF, false);
    } else if (type == boolean.class) {
      visitor.visitMethodInsn(INVOKESTATIC, BOOL_NAME, "valueOf", BVALUEOF, false);
    }
  }

  private static void args(String internalName, MvcMethod metadata, MethodVisitor method,
      Parameter[] parameters) {
    method.visitParameter("ctx", 0);
    method.visitCode();

    Label sourceStart = new Label();
    method.visitLabel(sourceStart);
    method.visitLineNumber(metadata.getLine(), sourceStart);

    if (parameters.length == 0) {
      method.visitInsn(ICONST_0);
      method.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    } else {
      int iconstbase = ICONST_0;
      int iconstlimt = 5;
      // This is for coroutines, we need to ignore last argument
      int paramCount = metadata.isSuspendFunction() ?
          parameters.length - 1 :
          parameters.length;

      if (paramCount > iconstlimt) {
        method.visitIntInsn(BIPUSH, paramCount);
      } else {
        method.visitInsn(iconstbase + paramCount);
      }
      method.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      if (paramCount > 0) {
        method.visitInsn(DUP);

        for (int i = 0; i < paramCount; i++) {
          if (i > 0) {
            method.visitInsn(DUP);
          }
          Parameter parameter = parameters[i];
          if (i <= iconstlimt) {
            method.visitInsn(iconstbase + i);
          } else {
            method.visitIntInsn(BIPUSH, i);
          }
          invokeTryParam(internalName, method, parameter, metadata.getParameterName(i));
          primitiveToWrapper(method, parameter.getType());
          method.visitInsn(AASTORE);
        }
      }
    }
    method.visitInsn(ARETURN);
    method.visitMaxs(0, 0);
    method.visitEnd();
  }

  private static void invokeTryParam(String internalName, MethodVisitor visitor,
      Parameter parameter, String paramName) {
    visitor.visitVarInsn(ALOAD, 0);
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitLdcInsn(provisionError(parameter));
    visitor.visitMethodInsn(INVOKESPECIAL, internalName, paramName,
        "(Lio/jooby/Context;Ljava/lang/String;)" + Type.getType(parameter.getType())
            .getDescriptor(), false);
  }

  private static String provisionError(Parameter parameter) {
    return "Unable to provision parameter: '" + parameter.getName() + ": " + parameter
        .getParameterizedType().getTypeName() + "'";
  }

  private static void tryParam(MvcMethod metadata, ClassWriter writer, Parameter parameter,
      int index) {
    Class type = parameter.getType();
    MethodVisitor visitor = writer.visitMethod(ACC_PRIVATE, metadata.getParameterName(index),
        "(Lio/jooby/Context;Ljava/lang/String;)" + Type.getType(type).getDescriptor(),
        null, null);
    visitor.visitParameter("ctx", 0);
    visitor.visitParameter("desc", 0);
    visitor.visitCode();
    Label sourceStart = new Label();
    visitor.visitLabel(sourceStart);
    visitor.visitLineNumber(metadata.getLine(), sourceStart);

    Label label0 = new Label();
    Label label1 = new Label();
    Label label2 = new Label();
    visitor.visitTryCatchBlock(label0, label1, label2, PROVISIONING);
    Label label3 = new Label();
    visitor.visitTryCatchBlock(label0, label1, label3, "java/lang/Exception");
    visitor.visitLabel(label0);

    tryParamBlock(metadata, visitor, parameter, index);

    visitor.visitLabel(label1);
    if (isIntType(type)) {
      visitor.visitInsn(IRETURN);
    } else if (type == long.class) {
      visitor.visitInsn(LRETURN);
    } else if (type == float.class) {
      visitor.visitInsn(FRETURN);
    } else if (parameter.getType() == double.class) {
      visitor.visitInsn(DRETURN);
    } else {
      visitor.visitInsn(ARETURN);
    }
    visitor.visitLabel(label2);
    visitor
        .visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{PROVISIONING});
    visitor.visitVarInsn(ASTORE, 3);
    visitor.visitVarInsn(ALOAD, 3);
    visitor.visitInsn(ATHROW);
    visitor.visitLabel(label3);
    visitor.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/lang/Exception"});
    visitor.visitVarInsn(ASTORE, 3);
    visitor.visitTypeInsn(NEW, PROVISIONING);
    visitor.visitInsn(DUP);
    visitor.visitVarInsn(ALOAD, 2);
    visitor.visitVarInsn(ALOAD, 3);
    visitor.visitMethodInsn(INVOKESPECIAL, PROVISIONING, "<init>",
        "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
    visitor.visitInsn(ATHROW);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private static void tryParamBlock(MvcMethod method, MethodVisitor visitor, Parameter parameter,
      int index) {
    Class<?> paramClass = parameter.getType();
    java.lang.reflect.Type paramType = parameter.getParameterizedType();
    boolean isSimple = ValueInjector.isSimple(paramClass, paramType);

    String name = method.getParameterName(index);

    visitor.visitVarInsn(ALOAD, ALOAD_CTX);

    BiConsumer<String, String> varaccess = isSimple ?
        (varname, type) -> {
          visitor.visitLdcInsn(varname);
          visitor.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, type,
              getMethodDescriptor(VALUE, STRING), true);
        }
        : NOOP;
    String httpType = httpType(method.getModel(), parameter, name, varaccess);

    Consumer<MethodVisitor> checkcast = checkCast(visitor, paramClass);

    if (paramClass != Context.class) {
      if (paramClass == String.class) {
        String source = "Value";
        if (httpType == null) {
          source = "Body";
          visitor.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "body", "()Lio/jooby/Body;",
              true);
        }
        visitor.visitInsn(ACONST_NULL);
        visitor.visitTypeInsn(CHECKCAST, "java/lang/String");
        visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/" + source, "value",
            "(Ljava/lang/String;)Ljava/lang/String;", true);
      } else if (paramClass.isPrimitive()) {
        String source = "Value";
        if (httpType == null) {
          source = "Body";
          visitor.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "body", "()Lio/jooby/Body;",
              true);
        }
        String valueMethod = paramClass.getSimpleName() + "Value";
        String valueDescriptor = "()" + Type.getDescriptor(paramClass);
        visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/" + source, valueMethod, valueDescriptor,
            true);
      } else if (paramClass == QueryString.class) {
        visitor.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "query", "()Lio/jooby/QueryString;",
            true);
      } else if (paramClass == Formdata.class) {
        visitor.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "form", "()Lio/jooby/Formdata;",
            true);
      } else if (paramClass == Multipart.class) {
        visitor
            .visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "multipart", "()Lio/jooby/Multipart;",
                true);
      } else if (paramClass == FlashMap.class) {
        visitor
            .visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "flash", "()Lio/jooby/FlashMap;",
                true);
      } else {
        String source;
        String convert;
        if (isSimple && httpType != null) {
          source = VALUE_NAME;
          convert = "to";
        } else {
          source = CTX_INTERNAL;
          convert = httpType == null ? "body" : httpType;
        }

        if (paramType == paramClass) {
          // raw parameter
          visitor.visitMethodInsn(INVOKEINTERFACE, source, convert,
              "(Ljava/lang/Class;)Ljava/lang/Object;", true);
        } else {
          dumpParameterType(visitor, parameter);
          visitor.visitMethodInsn(INVOKEINTERFACE, source, convert,
              "(Lio/jooby/Reified;)Ljava/lang/Object;", true);
        }
      }
    }

    if (checkcast != null) {
      checkcast.accept(visitor);
    }
  }

  private static Consumer<MethodVisitor> checkCast(MethodVisitor apply, Class<?> paramClass) {
    if (paramClass.isPrimitive() || paramClass == String.class) {
      return null;
    }
    if (!NATIVE.contains(paramClass)) {
      Type paramAsmType = getType(paramClass);
      apply.visitLdcInsn(paramAsmType);
      return visitor -> visitor.visitTypeInsn(CHECKCAST, paramAsmType.getInternalName());
    }
    return null;
  }

  private static String httpType(MvcAnnotation model, Parameter parameter, String name,
      BiConsumer<String, String> consumer) {
    if (model.isPathParam(parameter)) {
      consumer.accept(paramName(model, parameter, name), "path");
      return "path";
    } else if (model.isQueryParam(parameter)) {
      consumer.accept(paramName(model, parameter, name), "query");
      return "query";
    } else if (model.isHeaderParam(parameter)) {
      consumer.accept(paramName(model, parameter, name), "header");
      return "header";
    } else if (model.isFormParam(parameter)) {
      consumer.accept(paramName(model, parameter, name), "multipart");
      return "multipart";
    } else if (model.isCookieParam(parameter)) {
      consumer.accept(paramName(model, parameter, name), "cookie");
      return "cookie";
    } else if (model.isFlashParam(parameter)) {
      consumer.accept(paramName(model, parameter, name), "flash");
      return "flash";
    }

    return null;
  }

  private static void dumpParameterType(MethodVisitor apply, Parameter parameter) {
    ParameterizedType generic = (ParameterizedType) parameter.getParameterizedType();
    java.lang.reflect.Type[] arguments = generic.getActualTypeArguments();
    if (arguments.length < 5) {
      apply.visitInsn(ICONST_0 + arguments.length);
    } else {
      apply.visitIntInsn(BIPUSH, arguments.length);
    }
    apply.visitTypeInsn(ANEWARRAY, "java/lang/reflect/Type");

    for (int pos = 0; pos < arguments.length; pos++) {
      Class argument = $Types.parameterizedType0(arguments[pos]);
      apply.visitInsn(DUP);
      if (pos > 5) {
        apply.visitIntInsn(BIPUSH, pos);
      } else {
        apply.visitInsn(ICONST_0 + pos);
      }
      apply.visitLdcInsn(Type.getType(argument));
      apply.visitInsn(AASTORE);
    }

    apply.visitMethodInsn(INVOKESTATIC, "io/jooby/Reified", "getParameterized",
        "(Ljava/lang/reflect/Type;[Ljava/lang/reflect/Type;)Lio/jooby/Reified;", false);
  }

  private static String paramName(MvcAnnotation annotation, Parameter parameter, String defaults) {
    String name = annotation.getName(parameter);
    return name == null ? defaults : name;
  }

  private static String providerOf(Type owner) {
    StringBuilder signature = new StringBuilder(PROVIDER.getDescriptor());
    signature.insert(signature.length() - 1, "<" + owner.getDescriptor() + ">");
    return signature.toString();
  }

  private static boolean isIntType(Class type) {
    return type == int.class || type == boolean.class || type == byte.class || type == short.class;
  }

  public static Route.Handler newHandler(ClassLoader loader, MvcMethod metadata, Provider provider)
      throws Exception {
    Class<? extends MvcHandler> handler = compileClass(metadata);
    MvcHandler instance = handler.getDeclaredConstructor(Provider.class)
        .newInstance(provider);
    if (metadata.isSuspendFunction()) {
      Class coroutine = loader.loadClass("io.jooby.internal.mvc.CoroutineHandler");
      return (Route.Handler) coroutine
          .getDeclaredConstructor(Provider.class, Method.class, MvcHandler.class)
          .newInstance(provider, metadata.method, instance);
    }
    return instance;
  }
}
