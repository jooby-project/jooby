package io.jooby.compiler;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.ProvisioningException;
import io.jooby.QueryString;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.Session;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.Value;
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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
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
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

public class MvcHandlerCompiler {

  enum ParamType {
    OPTIONAL {
      @Override public void withReified(SneakyThrows.Consumer<String> consumer) {
        consumer.accept(name().toLowerCase());
      }
    },
    LIST {
      @Override public void withReified(SneakyThrows.Consumer<String> consumer) {
        consumer.accept(name().toLowerCase());
      }
    },
    SET {
      @Override public void withReified(SneakyThrows.Consumer<String> consumer) {
        consumer.accept(name().toLowerCase());
      }
    },
    FILE_UPLOAD,
    BODY,
    SIMPLE,
    OBJECT;

    public boolean isOptional() {
      return this == OPTIONAL;
    }

    public void withReified(SneakyThrows.Consumer<String> consumer) {
    }

    public boolean isList() {
      return this == LIST;
    }

    public boolean isSet() {
      return this == SET;
    }

    public boolean isFileUpload() {
      return this == FILE_UPLOAD;
    }

    public boolean isBody() {
      return this == BODY;
    }
  }

  private static final Map<String, Set<String>> PARAMS = new HashMap<>();

  static {
    PARAMS.put("path", Annotations.PATH_PARAMS);
    PARAMS.put("query", Annotations.QUERY_PARAMS);
    PARAMS.put("cookie", Annotations.COOKIE_PARAMS);
    PARAMS.put("header", Annotations.HEADER_PARAMS);
    PARAMS.put("flash", Annotations.FLASH_PARAMS);
    PARAMS.put("multipart", Annotations.FORM_PARAMS);
  }

  private static final Type OBJ = getType(Object.class);
  private static final Type HANDLER = getType(Route.Handler.class);
  private static final Type STATUS_CODE = getType(StatusCode.class);

  private static final Type PROVIDER = getType(Provider.class);
  private static final String PROVIDER_VAR = "provider";
  private static final String PROVIDER_DESCRIPTOR = getMethodDescriptor(OBJ);

  private static final Type CTX = getType(Context.class);

  private static final String APPLY_DESCRIPTOR = getMethodDescriptor(OBJ, CTX);
  private static final String[] APPLY_THROWS = new String[]{getInternalName(Exception.class)};

  private final TypeElement owner;
  private final ExecutableElement executable;
  private final ProcessingEnvironment environment;
  private final String httpMethod;
  private Map<String, SneakyThrows.Consumer2<String, ClassWriter>> additionalMethods = new LinkedHashMap<>();

  public MvcHandlerCompiler(ProcessingEnvironment environment, String httpMethod,
      ExecutableElement executable) {
    this.httpMethod = httpMethod.toLowerCase();
    this.environment = environment;
    this.executable = executable;
    this.owner = (TypeElement) executable.getEnclosingElement();
  }

  public byte[] compile() throws Exception {
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    // public class Controller$methodName implements Route.Handler {
    writer.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, getHandlerInternal(), null,
        OBJ.getInternalName(),
        new String[]{
            HANDLER.getInternalName()
        });

    writer.visitSource(getSourceFileName(), null);

    writer.visitInnerClass(HANDLER.getInternalName(), getInternalName(Route.class),
        Route.Handler.class.getSimpleName(),
        ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE);

    // Constructor(Provider<Controller> provider)
    constructor(writer);

    /** Apply implementation: */
    apply(writer);

    for (Map.Entry<String, SneakyThrows.Consumer2<String, ClassWriter>> additionalMethod : additionalMethods
        .entrySet()) {
      additionalMethod.getValue().accept(additionalMethod.getKey(), writer);
    }

    writer.visitEnd();
    return writer.toByteArray();
  }

  private void apply(ClassWriter writer) throws Exception {
    Type owner = asmType(getOwner());
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
    apply.visitFieldInsn(GETFIELD, getHandlerInternal(), PROVIDER_VAR, PROVIDER.getDescriptor());
    apply.visitMethodInsn(INVOKEINTERFACE, PROVIDER.getInternalName(), "get", PROVIDER_DESCRIPTOR,
        true);
    apply.visitTypeInsn(CHECKCAST, owner.getInternalName());

    /** Arguments. */
    processArguments(apply);

    /** Invoke. */
    apply.visitMethodInsn(INVOKEVIRTUAL, owner.getInternalName(), methodName, methodDescriptor,
        false);

    processReturnType(apply);

    apply.visitEnd();
  }

  private void processReturnType(MethodVisitor visitor) throws Exception {
    TypeKind kind = executable.getReturnType().getKind();
    if (kind == TypeKind.VOID) {
      visitor.visitVarInsn(ALOAD, 1);

      // Make sure controller method doesn't inject Context.
      boolean sideEffect = executable.getParameters().stream().map(VariableElement::asType)
          .map(this::rawType)
          .anyMatch(type -> type.equals(Context.class.getName()));

      // It does inject. Assume response is generated via side effect (don't generate 204)
      if (!sideEffect) {
        visitor
            .visitFieldInsn(GETSTATIC, STATUS_CODE.getInternalName(), "NO_CONTENT",
                STATUS_CODE.getDescriptor());
        Method sendStatusCode = Context.class.getDeclaredMethod("send", StatusCode.class);
        visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), sendStatusCode.getName(),
            getMethodDescriptor(sendStatusCode), true);
      }
    } else {

      Method wrapper;
      switch (kind) {
        case CHAR:
          wrapper = Character.class.getDeclaredMethod("valueOf", Character.TYPE);
          break;
        case BYTE:
          wrapper = Byte.class.getDeclaredMethod("valueOf", Byte.TYPE);
          break;
        case SHORT:
          wrapper = Short.class.getDeclaredMethod("valueOf", Short.TYPE);
          break;
        case INT:
          wrapper = Integer.class.getDeclaredMethod("valueOf", Integer.TYPE);
          break;
        case LONG:
          wrapper = Long.class.getDeclaredMethod("valueOf", Long.TYPE);
          break;
        case FLOAT:
          wrapper = Float.class.getDeclaredMethod("valueOf", Float.TYPE);
          break;
        case DOUBLE:
          wrapper = Double.class.getDeclaredMethod("valueOf", Double.TYPE);
          break;
        default:
          wrapper = null;
      }
      if (wrapper == null) {
        String rawType = rawType(executable.getReturnType());
        if (eq(StatusCode.class, rawType)) {
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

  private void processArguments(MethodVisitor visitor) throws Exception {
    for (VariableElement parameter : executable.getParameters()) {
      /**
       * Load context
       */
      visitor.visitVarInsn(ALOAD, 1);

      String rawType = rawType(arg0(parameter.asType()));
      if (isTypeInjection(rawType)) {
        ParamType paramType = paramType(parameter);
        if (!isContext(rawType)) {
          Method method = typeInjection(rawType, paramType);
          visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), method.getName(),
              Type.getMethodDescriptor(method), true);
        }
        if (paramType.isOptional()) {
          visitor.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "ofNullable",
              "(Ljava/lang/Object;)Ljava/util/Optional;", false);
        }
      } else {
        ParamType paramType = paramType(parameter);
        if (paramType.isFileUpload()) {
          if (eq(List.class, rawType(parameter.asType()))) {
            additionalMethods.put(parameter.getSimpleName().toString(), this::files);
            visitor.visitMethodInsn(INVOKESTATIC, getHandlerInternal(),
                parameter.getSimpleName().toString(),
                "(Lio/jooby/Context;)Ljava/util/List;", false);
          } else {
            visitor.visitLdcInsn(parameter.getSimpleName().toString());

            Method fileMethod = Context.class.getDeclaredMethod("file", String.class);

            visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), fileMethod.getName(),
                getMethodDescriptor(fileMethod), true);

            if (eq(Path.class, rawType(parameter.asType()))) {
              Method fileUpload = FileUpload.class.getDeclaredMethod("path");
              visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/FileUpload", fileUpload.getName(),
                  getMethodDescriptor(fileUpload), true);
            }
          }
        } else {
          Map.Entry<String, Set<String>> strategy = paramStrategy(parameter);

          String parameterName = parameterName(parameter, strategy.getValue());

          Method paramValue = paramValue(parameter, paramType);
          boolean dynamic =
              paramValue.getName().equals("to") && !isSimpleType(arg0(parameter.asType()));
          if (dynamic) {
            Method paramMethod = Context.class.getDeclaredMethod(strategy.getKey());
            visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
                getMethodDescriptor(paramMethod), true);

//            methodVisitor.visitVarInsn(ALOAD, 1);
//            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Context", "body", "()Lio/jooby/Body;", true);
//            methodVisitor.visitLdcInsn(Type.getType("Lio/jooby/internal/mvc/QPoint;"));
//            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", "to", "(Ljava/lang/Class;)Ljava/lang/Object;", true);
//            methodVisitor.visitTypeInsn(CHECKCAST, "io/jooby/internal/mvc/QPoint");
//            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/internal/mvc/Poc", "getIt", "(Lio/jooby/internal/mvc/QPoint;)Ljava/lang/String;", false);

            if (strategy.getKey().equals("body") && rawType.equals("byte[]")) {
              Method bytes = Body.class.getDeclaredMethod("bytes");
              visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", bytes.getName(),
                  getMethodDescriptor(bytes), true);
            } else if (strategy.getKey().equals("body") && eq(InputStream.class, rawType)) {
              Method stream = Body.class.getDeclaredMethod("stream");
              visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", stream.getName(),
                  getMethodDescriptor(stream), true);
            } else if (strategy.getKey().equals("body") && eq(ReadableByteChannel.class, rawType)) {
              Method channel = Body.class.getDeclaredMethod("channel");
              visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Body", channel.getName(),
                  getMethodDescriptor(channel), true);
            } else {
              visitor.visitLdcInsn(asmType(rawType));

              /** to(Reified): */
              paramType.withReified(name -> {
                Method reified = Reified.class
                    .getDeclaredMethod(name, java.lang.reflect.Type.class);
                visitor.visitMethodInsn(INVOKESTATIC, "io/jooby/Reified", reified.getName(),
                    getMethodDescriptor(reified), false);
              });

              String valueOwner = strategy.getKey().equals("body") ? "io/jooby/Body" : "io/jooby/Value";
              visitor.visitMethodInsn(INVOKEINTERFACE, valueOwner, paramValue.getName(),
                  getMethodDescriptor(paramValue), true);
              visitor.visitTypeInsn(CHECKCAST, asmType(rawType(parameter)).getInternalName());
            }
          } else {
            try {
              /** param(String) vs body() */
              Method paramMethod = Context.class.getDeclaredMethod(strategy.getKey(), String.class);
              visitor.visitLdcInsn(parameterName);
              visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
                  getMethodDescriptor(paramMethod), true);
            } catch (NoSuchMethodException x) {
              /** must be: body() */
              Method paramMethod = Context.class.getDeclaredMethod(strategy.getKey());
              visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), paramMethod.getName(),
                  getMethodDescriptor(paramMethod), true);
            }
            // to(Class)
            boolean toClass = paramValue.getName().equals("to");
            // toOptional(Class) or toList(Class) or toSet(Class)
            boolean toOptColClass =
                (paramType.isOptional() || paramType.isList() || paramType.isSet()) && !eq(
                    String.class, rawType);
            if (toOptColClass || toClass) {
              visitor.visitLdcInsn(asmType(rawType));
            }
            visitor.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Value", paramValue.getName(),
                getMethodDescriptor(paramValue), true);
            if (toClass) {
              visitor.visitTypeInsn(CHECKCAST, asmType(rawType(parameter)).getInternalName());
            }
          }
        }
      }
    }
  }

  /**
   * Generate a files method like:
   *
   * <pre>{@code
   *
   *  List<FileUpload> [paramName](Context ctx) {
   *     List<FileUpload> files = ctx.files("[paramName]");
   *     return files.isEmpty() ? ctx.files() : files;
   *  }
   *
   * }</pre>
   *
   *
   * @param parameter
   * @param writer
   * @throws Exception
   */
  private void files(String parameter, ClassWriter writer) throws Exception {
    MethodVisitor visitor = writer
        .visitMethod(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, parameter,
            "(Lio/jooby/Context;)Ljava/util/List;",
            "(Lio/jooby/Context;)Ljava/util/List<Lio/jooby/FileUpload;>;", null);
    visitor.visitParameter("ctx", 0);
    visitor.visitCode();
    visitor.visitVarInsn(ALOAD, 0);
    visitor.visitLdcInsn(parameter);
    Method filesWithName = Context.class.getDeclaredMethod("files", String.class);
    visitor.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), filesWithName.getName(),
        getMethodDescriptor(filesWithName), true);
    visitor.visitVarInsn(ASTORE, 1);
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true);
    Label label0 = new Label();
    visitor.visitJumpInsn(IFEQ, label0);
    visitor.visitVarInsn(ALOAD, 0);
    Method allFiles = Context.class.getDeclaredMethod("files");
    visitor
        .visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), allFiles.getName(),
            getMethodDescriptor(allFiles), true);
    Label label1 = new Label();
    visitor.visitJumpInsn(GOTO, label1);
    visitor.visitLabel(label0);
    visitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/util/List"}, 0, null);
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitLabel(label1);
    visitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/util/List"});
    visitor.visitInsn(ARETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private Map.Entry<String, Set<String>> paramStrategy(VariableElement parameter) {
    return PARAMS.entrySet().stream()
        .filter(it -> isParam(parameter, it.getValue()))
        .findFirst()
        .orElseGet(() -> new Map.Entry<String, Set<String>>() {
          @Override public String getKey() {
            return "body";
          }

          @Override public Set<String> getValue() {
            return Collections.emptySet();
          }

          @Override public Set<String> setValue(Set<String> value) {
            return null;
          }
        });
  }

  private ParamType paramType(VariableElement parameter) {
    TypeMirror typeMirror = parameter.asType();
    paramStrategy(parameter);
    if (isSimpleType(typeMirror)) {
      return ParamType.SIMPLE;
    }
    Types types = environment.getTypeUtils();
    TypeMirror erasure = types.erasure(typeMirror);
    if (eq(Optional.class, erasure)) {
      return ParamType.OPTIONAL;
    }
    if (eq(FileUpload.class, erasure) || (eq(List.class, erasure) && eq(FileUpload.class,
        arg0(typeMirror)))) {
      return ParamType.FILE_UPLOAD;
    }
    if (eq(Path.class, erasure)) {
      return ParamType.FILE_UPLOAD;
    }
    if (eq(List.class, erasure)) {
      return ParamType.LIST;
    }
    if (eq(Set.class, erasure)) {
      return ParamType.SET;
    }
    return ParamType.OBJECT;
  }

  private Method paramValue(VariableElement param, ParamType paramType)
      throws Exception {
    TypeMirror type = param.asType();
    if (eq(String.class, type)) {
      return Value.class.getDeclaredMethod("value");
    }
    if (eq(int.class, type)) {
      return Value.class.getDeclaredMethod("intValue");
    }
    if (eq(byte.class, type)) {
      return Value.class.getDeclaredMethod("byteValue");
    }
    if (eq(long.class, type)) {
      return Value.class.getDeclaredMethod("longValue");
    }
    if (eq(float.class, type)) {
      return Value.class.getDeclaredMethod("floatValue");
    }
    if (eq(double.class, type)) {
      return Value.class.getDeclaredMethod("doubleValue");
    }
    if (eq(boolean.class, type)) {
      return Value.class.getDeclaredMethod("booleanValue");
    }
    if (paramType.isOptional()) {
      if (eq(String.class, arg0(type))) {
        return Value.class.getDeclaredMethod("toOptional");
      } else if (isSimpleType(arg0(type))) {
        return Value.class.getDeclaredMethod("toOptional", Class.class);
      } else {
        return Value.class.getDeclaredMethod("to", Reified.class);
      }
    } else if (paramType.isList()) {
      if (eq(String.class, arg0(type))) {
        return Value.class.getDeclaredMethod("toList");
      } else if (isSimpleType(arg0(type))) {
        return Value.class.getDeclaredMethod("toList", Class.class);
      } else {
        return Value.class.getDeclaredMethod("to", Reified.class);
      }
    } else if (paramType.isSet()) {
      if (eq(String.class, arg0(type))) {
        return Value.class.getDeclaredMethod("toSet");
      } else if (isSimpleType(arg0(type))) {
        return Value.class.getDeclaredMethod("toSet", Class.class);
      } else {
        return Value.class.getDeclaredMethod("to", Reified.class);
      }
    } else {
      if (isSimpleType(arg0(type))) {
        return Value.class.getDeclaredMethod("to", Class.class);
      } else {
        // TODO: check generic type
        return Value.class.getDeclaredMethod("to", Class.class);
      }
    }
  }

  private boolean isSimpleType(TypeMirror mirror) {
    return eq(String.class, mirror) || mirror.getKind().isPrimitive() || isBasic(mirror);
  }

  private boolean isBasic(TypeMirror mirror) {
    switch (rawType(mirror)) {
      case "java.lang.Byte":
      case "java.lang.Character":
      case "java.lang.Short":
      case "java.lang.Integer":
      case "java.lang.Long":
      case "java.lang.Float":
      case "java.lang.Double":
      case "java.time.Instant":
      case "java.util.UUID":
      case "java.nio.File":
      case "java.math.BigDecimal":
      case "java.math.BigInteger":
      case "java.nio.charset.Charset":
      case "io.jooby.StatusCode":
        return true;
      default:
        return false;
    }
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

  private boolean eq(Class type, TypeMirror mirror) {
    return type.getName().equals(rawType(mirror));
  }

  private boolean eq(Class type, String typeString) {
    return type.getName().equals(typeString);
  }

  private String parameterName(VariableElement parameter, Set<String> types) {
    return annotations(parameter.getAnnotationMirrors(), types).stream()
        .flatMap(it -> annotationAttribute(it, "value").stream())
        .findFirst()
        .orElse(parameter.getSimpleName().toString());
  }

  private List<String> annotationAttribute(AnnotationMirror mirror, String name) {
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
        .getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().toString().equals(name)) {
        Object value = entry.getValue().getValue();
        if (value instanceof List) {
          List values = (List) value;
          return (List<String>) values.stream()
              .map(it -> cleanString(it.toString()))
              .collect(Collectors.toList());
        }
        return Collections.singletonList(cleanString(value.toString()));
      }
    }
    return Collections.emptyList();
  }

  private String cleanString(String value) {
    if (value.length() > 0 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  private boolean isParam(VariableElement parameter, Set<String> annotations) {
    return annotations(parameter.getAnnotationMirrors(), annotations).size() > 0;
  }

  private List<AnnotationMirror> annotations(List<? extends AnnotationMirror> annotationMirrors,
      Set<String> types) {
    return annotationMirrors.stream()
        .filter(it -> {
          String rawType = rawType(it.getAnnotationType());
          return types.contains(rawType);
        })
        .collect(Collectors.toList());
  }

  private Method typeInjection(String rawType, ParamType paramType) throws Exception {
    if (QueryString.class.getName().equals(rawType)) {
      return Context.class.getDeclaredMethod("query");
    }
    if (Formdata.class.getName().equals(rawType)) {
      return Context.class.getDeclaredMethod("form");
    }
    if (Multipart.class.getName().equals(rawType)) {
      return Context.class.getDeclaredMethod("multipart");
    }
    if (FlashMap.class.getName().equals(rawType)) {
      return Context.class.getDeclaredMethod("flash");
    }
    if (Session.class.getName().equals(rawType)) {
      if (paramType.isOptional()) {
        return Context.class.getDeclaredMethod("sessionOrNull");
      } else {
        return Context.class.getDeclaredMethod("session");
      }
    }
    throw new IllegalArgumentException("Unsupported type injection: " + rawType);
  }

  private boolean isTypeInjection(String rawType) {
    if (Context.class.getName().equals(rawType)) {
      return true;
    }
    if (QueryString.class.getName().equals(rawType)) {
      return true;
    }
    if (Formdata.class.getName().equals(rawType)) {
      return true;
    }
    if (Multipart.class.getName().equals(rawType)) {
      return true;
    }
    if (FlashMap.class.getName().equals(rawType)) {
      return true;
    }
    if (Session.class.getName().equals(rawType)) {
      return true;
    }
    return false;
  }

  private boolean isContext(String rawType) {
    return Context.class.getName().equals(rawType);
  }

  private void constructor(ClassWriter writer) {
    String provider = providerOf(asmType(getOwner()));

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
    constructor
        .visitFieldInsn(PUTFIELD, getHandlerInternal(), PROVIDER_VAR, PROVIDER.getDescriptor());
    constructor.visitInsn(RETURN);
    constructor.visitMaxs(0, 0);
    constructor.visitEnd();
  }

  public String getSourceFileName() {
    return owner.getSimpleName() + ".java";
  }

  public String getHandlerName() {
    return getOwner() + "$" + httpMethod.toUpperCase() + "$" + executable.getSimpleName();
  }

  public String getHandlerInternal() {
    return getHandlerName().replace(".", "/");
  }

  public String getOwner() {
    return owner.getQualifiedName().toString();
  }

  public String getKey() {
    return getOwner() + "." + executable.getSimpleName() + methodDescriptor();

  }

  private String methodDescriptor() {
    Type returnType = asmType(rawType(executable.getReturnType()));
    Type[] args = asmTypes(rawTypes(executable.getParameters()));
    return Type.getMethodDescriptor(returnType, args);
  }

  private String[] rawTypes(List<? extends VariableElement> parameters) {
    return parameters.stream()
        .map(VariableElement::asType)
        .map(this::rawType)
        .toArray(String[]::new);
  }

  private String rawType(VariableElement var) {
    return rawType(var.asType());
  }

  private String rawType(TypeMirror type) {
    return environment.getTypeUtils().erasure(type).toString();
  }

  private Type asmType(String type) {
    switch (type) {
      case "byte":
        return Type.BYTE_TYPE;
      case "byte[]":
        return Type.getType(byte[].class);
      case "int":
        return Type.INT_TYPE;
      case "int[]":
        return Type.getType(int[].class);
      case "long":
        return Type.LONG_TYPE;
      case "long[]":
        return Type.getType(long[].class);
      case "float":
        return Type.FLOAT_TYPE;
      case "float[]":
        return Type.getType(float[].class);
      case "double":
        return Type.DOUBLE_TYPE;
      case "double[]":
        return Type.getType(double[].class);
      case "boolean":
        return Type.BOOLEAN_TYPE;
      case "boolean[]":
        return Type.getType(boolean[].class);
      case "void":
        return Type.VOID_TYPE;
      case "short":
        return Type.SHORT_TYPE;
      case "short[]":
        return Type.getType(short[].class);
      case "char":
        return Type.CHAR_TYPE;
      case "char[]":
        return Type.getType(char[].class);
      case "String":
        return Type.getType(String.class);
      case "String[]":
        return Type.getType(String[].class);
      default:
        String prefix = "";
        if (type.endsWith("[]")) {
          prefix = "[";
        }
        return Type.getObjectType(prefix + type.replace(".", "/"));
    }
  }

  private Type[] asmTypes(String... types) {
    return Stream.of(types)
        .map(this::asmType)
        .toArray(Type[]::new);
  }

  private static TypeMirror arg0(TypeMirror mirror) {
    if (mirror instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) mirror;
      List<? extends TypeMirror> args = declaredType.getTypeArguments();
      return args.isEmpty() ? declaredType : arg0(args.get(0));
    }
    return mirror;
  }

  private static String providerOf(Type owner) {
    StringBuilder signature = new StringBuilder(PROVIDER.getDescriptor());
    signature.insert(signature.length() - 1, "<" + owner.getDescriptor() + ">");
    return signature.toString();
  }
}
