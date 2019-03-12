package io.jooby.internal.mvc;

import io.jooby.Context;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Value;
import io.jooby.annotations.FormParam;
import io.jooby.annotations.HeaderParam;
import io.jooby.annotations.PathParam;
import io.jooby.annotations.QueryParam;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.F_SAME1;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
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
  public static final String PROVIDER_VAR = "provider";
  public static final String PROVIDER_DESCRIPTOR = getMethodDescriptor(OBJ);

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

  private static final String[] HANDLER_IMPLEMENTS = new String[]{
      getInternalName(Route.Handler.class)};

  private static final int ALOAD_CTX = 1;

  private static final int ALOAD_OWNER = 2;

  private static final BiConsumer<String, String> NOOP = (varname, type) -> {
  };

  public static Class<? extends Route.Handler> compileClass(MvcMethod method)
      throws ClassNotFoundException {
    byte[] bytes = compile(method);
    return (Class<? extends Route.Handler>) new ClassLoader() {
      @Override protected Class<?> findClass(String name) {
        return defineClass(name, bytes, 0, bytes.length);
      }
    }.loadClass(method.getHandlerName());
  }

  public static byte[] compile(MvcMethod method) {
    Method m = method.getMethod();
    String descriptor = getMethodDescriptor(m);
    Type owner = getType(m.getDeclaringClass());
    String internalName = method.getHandlerName().replace(".", "/");
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    writer.visit(V1_8, ACC_PUBLIC | ACC_SUPER, internalName, null, OBJ.getInternalName(),
        HANDLER_IMPLEMENTS);

    writer.visitSource(method.getSource(), null);

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

    /**
     * Apply implementation:
     */
    MethodVisitor apply = writer
        .visitMethod(ACC_PUBLIC, "apply", APPLY_DESCRIPTOR, null, APPLY_THROWS);
    apply.visitParameter("ctx", 0);
    apply.visitCode();

    // try
    Label label0 = new Label();
    Label label1 = new Label();
    Label label2 = new Label();
    apply.visitTryCatchBlock(label0, label1, label2, "io/jooby/Err$Missing");
    Label label3 = new Label();
    apply.visitTryCatchBlock(label0, label1, label3, "io/jooby/Err$TypeMismatch");

    apply.visitLabel(label0);

    Label sourceStart = new Label();
    apply.visitLabel(sourceStart);
    apply.visitLineNumber(method.getLine(), sourceStart);
    apply.visitVarInsn(ALOAD, 0);
    apply.visitFieldInsn(GETFIELD, internalName, PROVIDER_VAR, PROVIDER.getDescriptor());
    apply.visitMethodInsn(INVOKEINTERFACE, PROVIDER.getInternalName(), "get", PROVIDER_DESCRIPTOR,
        true);
    apply.visitTypeInsn(CHECKCAST, getInternalName(method.method.getDeclaringClass()));

    Parameter[] parameters = method.method.getParameters();

    apply.visitVarInsn(ASTORE, ALOAD_OWNER);
    apply.visitVarInsn(ALOAD, ALOAD_OWNER);

    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      Class<?> paramClass = parameter.getType();
      java.lang.reflect.Type paramType = parameter.getParameterizedType();
      boolean isSimple = ValueInjector.isSimple(paramClass, paramType);

      String name = method.getParameterName(i);

      apply.visitVarInsn(ALOAD, ALOAD_CTX);

      BiConsumer<String, String> varaccess = isSimple ?
          (varname, type) -> {
            apply.visitLdcInsn(varname);
            apply.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, type,
                getMethodDescriptor(VALUE, STRING), true);
          }
          : NOOP;
      String httpType = httpType(parameter, name, varaccess);

      Consumer<MethodVisitor> checkcast = null;
      if (paramClass.isPrimitive()) {
        if (paramClass == int.class) {
          apply.visitFieldInsn(GETSTATIC, INT_NAME, "TYPE", "Ljava/lang/Class;");
          checkcast = visitor -> {
            visitor.visitTypeInsn(CHECKCAST, INT_NAME);
            visitor.visitMethodInsn(INVOKEVIRTUAL, INT_NAME, "intValue", "()I", false);
          };
        } else if (paramClass == long.class) {
          apply.visitFieldInsn(GETSTATIC, LONG_NAME, "TYPE", "Ljava/lang/Class;");
          checkcast = visitor -> {
            visitor.visitTypeInsn(CHECKCAST, LONG_NAME);
            visitor.visitMethodInsn(INVOKEVIRTUAL, LONG_NAME, "longValue", "()J", false);
          };
        } else if (paramClass == float.class) {
          apply.visitFieldInsn(GETSTATIC, FLOAT_NAME, "TYPE", "Ljava/lang/Class;");
          checkcast = visitor -> {
            visitor.visitTypeInsn(CHECKCAST, FLOAT_NAME);
            visitor.visitMethodInsn(INVOKEVIRTUAL, FLOAT_NAME, "floatValue", "()F", false);
          };
        } else if (paramClass == double.class) {
          apply.visitFieldInsn(GETSTATIC, DOUBLE_NAME, "TYPE", "Ljava/lang/Class;");
          checkcast = visitor -> {
            visitor.visitTypeInsn(CHECKCAST, DOUBLE_NAME);
            visitor.visitMethodInsn(INVOKEVIRTUAL, DOUBLE_NAME, "doubleValue", "()D", false);
          };
        } else if (paramClass == boolean.class) {
          apply.visitFieldInsn(GETSTATIC, BOOL_NAME, "TYPE", "Ljava/lang/Class;");
          checkcast = visitor -> {
            visitor.visitTypeInsn(CHECKCAST, BOOL_NAME);
            visitor.visitMethodInsn(INVOKEVIRTUAL, BOOL_NAME, "booleanValue", "()Z", false);
          };
        }
      } else {
        if (paramClass == Context.class || paramClass == QueryString.class
            || paramClass == Formdata.class || paramClass == Multipart.class) {
          // do nothing
        } else {
          Type paramAsmType = getType(paramClass);
          apply.visitLdcInsn(paramAsmType);
          checkcast = visitor -> visitor.visitTypeInsn(CHECKCAST, paramAsmType.getInternalName());
        }
      }

      if (paramClass != Context.class) {
        if (paramClass == QueryString.class) {
          apply.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "query", "()Lio/jooby/QueryString;",
              true);
        } else if (paramClass == Formdata.class) {
          apply.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "form", "()Lio/jooby/Formdata;",
              true);
        } else if (paramClass == Multipart.class) {
          apply
              .visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "multipart", "()Lio/jooby/Multipart;",
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
            apply.visitMethodInsn(INVOKEINTERFACE, source, convert,
                "(Ljava/lang/Class;)Ljava/lang/Object;", true);
          } else {
            dumpParameterType(apply, parameter);
            apply.visitMethodInsn(INVOKEINTERFACE, source, convert,
                "(Lio/jooby/Reified;)Ljava/lang/Object;", true);
          }
        }
      }

      if (checkcast != null) {
        checkcast.accept(apply);
      }
    }
    apply.visitMethodInsn(INVOKEVIRTUAL, owner.getInternalName(), m.getName(), descriptor, false);

    Class<?> returnType = m.getReturnType();
    if (returnType == void.class) {
      apply.visitVarInsn(ALOAD, 1);

      apply.visitFieldInsn(GETSTATIC, "io/jooby/StatusCode", "NO_CONTENT",
          "Lio/jooby/StatusCode;");
      apply.visitMethodInsn(INVOKEINTERFACE, CTX_INTERNAL, "sendStatusCode",
          "(Lio/jooby/StatusCode;)Lio/jooby/Context;", true);
      apply.visitInsn(Opcodes.POP);
      apply.visitVarInsn(ALOAD, 1);
    } else if (returnType == int.class) {
      apply.visitMethodInsn(INVOKESTATIC, INT_NAME, "valueOf", IVALUEOF, false);
    } else if (returnType == long.class) {
      apply.visitMethodInsn(INVOKESTATIC, LONG_NAME, "valueOf", LVALUEOF, false);
    } else if (returnType == float.class) {
      apply.visitMethodInsn(INVOKESTATIC, FLOAT_NAME, "valueOf", FVALUEOF, false);
    } else if (returnType == double.class) {
      apply.visitMethodInsn(INVOKESTATIC, DOUBLE_NAME, "valueOf", DVALUEOF, false);
    } else if (returnType == boolean.class) {
      apply.visitMethodInsn(INVOKESTATIC, BOOL_NAME, "valueOf", BVALUEOF, false);
    }

    // Writer was created with COMPUTE_MAXS
    apply.visitLabel(label1);
    apply.visitInsn(ARETURN);
    throwProvision(method, apply, parameters, "io/jooby/Err$Missing", label2);
    throwProvision(method, apply, parameters, "io/jooby/Err$TypeMismatch", label3);
    apply.visitMaxs(0, 0);
    apply.visitEnd();
    writer.visitEnd();
    return writer.toByteArray();
  }

  private static void throwProvision(MvcMethod method, MethodVisitor apply, Parameter[] params, String cathException, Label label) {
    apply.visitLabel(label);

    apply.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {cathException});

    apply.visitVarInsn(ASTORE, 2);
    apply.visitTypeInsn(NEW, "java/util/HashMap");
    apply.visitInsn(DUP);
    apply.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
    apply.visitVarInsn(ASTORE, 3);
    // dump debug parameter map
    for (int i = 0; i < params.length; i++) {
      Parameter parameter = params[i];

      apply.visitVarInsn(ALOAD, 3);
      String parameterName = method.getParameterName(i);
      StringBuilder name = new StringBuilder();
      httpType(parameter, parameterName, (n, t) -> name.append(name));
      if (name.length() == 0) {
        name.append(parameterName);
      }
      apply.visitLdcInsn(name.toString());
      apply.visitLdcInsn(parameter.getName() + ": " + parameter.getParameterizedType().getTypeName());
      apply.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
          "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
      apply.visitInsn(POP);
    }

    apply.visitVarInsn(ALOAD, 2);
    apply.visitMethodInsn(INVOKEVIRTUAL, cathException, "getParameter", "()Ljava/lang/String;", false);
    apply.visitVarInsn(ASTORE, 4);
    apply.visitVarInsn(ALOAD, 3);
    apply.visitVarInsn(ALOAD, 4);
    apply.visitVarInsn(ALOAD, 4);
    apply.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "getOrDefault", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
    apply.visitTypeInsn(CHECKCAST, "java/lang/String");
    apply.visitVarInsn(ASTORE, 5);
    apply.visitTypeInsn(NEW, "io/jooby/Err$Provisioning");
    apply.visitInsn(DUP);
    apply.visitTypeInsn(NEW, "java/lang/StringBuilder");
    apply.visitInsn(DUP);
    apply.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
    apply.visitLdcInsn("Unable to provision parameter: '");
    apply.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    apply.visitVarInsn(ALOAD, 5);
    apply.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    apply.visitLdcInsn("'");
    apply.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    apply.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    apply.visitVarInsn(ALOAD, 2);
    apply.visitMethodInsn(INVOKESPECIAL, "io/jooby/Err$Provisioning", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
    apply.visitInsn(ATHROW);
  }

  private static String httpType(Parameter parameter, String name,
      BiConsumer<String, String> consumer) {
    if (parameter.getAnnotation(PathParam.class) != null) {
      consumer.accept(paramName(parameter.getAnnotation(PathParam.class), name), "path");
      return "path";
    } else if (parameter.getAnnotation(QueryParam.class) != null) {
      consumer.accept(paramName(parameter.getAnnotation(QueryParam.class), name), "query");
      return "query";
    } else if (parameter.getAnnotation(HeaderParam.class) != null) {
      consumer.accept(paramName(parameter.getAnnotation(HeaderParam.class), name), "header");
      return "header";
    } else if (parameter.getAnnotation(FormParam.class) != null) {
      consumer.accept(paramName(parameter.getAnnotation(FormParam.class), name), "multipart");
      return "multipart";
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

  private static String paramName(PathParam annotation, String defaults) {
    String name = annotation.value().trim();
    return name.length() > 0 ? name : defaults;
  }

  private static String paramName(QueryParam annotation, String defaults) {
    String name = annotation.value().trim();
    return name.length() > 0 ? name : defaults;
  }

  private static String paramName(HeaderParam annotation, String defaults) {
    String name = annotation.value().trim();
    return name.length() > 0 ? name : defaults;
  }

  private static String paramName(FormParam annotation, String defaults) {
    String name = annotation.value().trim();
    return name.length() > 0 ? name : defaults;
  }

  private static String providerOf(Type owner) {
    StringBuilder signature = new StringBuilder(PROVIDER.getDescriptor());
    signature.insert(signature.length() - 1, "<" + owner.getDescriptor() + ">");
    return signature.toString();
  }
}
