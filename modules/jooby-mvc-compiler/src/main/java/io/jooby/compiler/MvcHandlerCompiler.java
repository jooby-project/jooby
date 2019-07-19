package io.jooby.compiler;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.jooby.Context;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.ProvisioningException;
import io.jooby.QueryString;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.Session;
import io.jooby.SneakyThrows;
import io.jooby.Value;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
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

    public boolean isSimple() {
      return this == SIMPLE;
    }

    public boolean isObject() {
      return this == OBJECT;
    }

    public TypeMirror arg0(TypeMirror mirror) {
      if (mirror instanceof DeclaredType) {
        DeclaredType declaredType = (DeclaredType) mirror;
        List<? extends TypeMirror> args = declaredType.getTypeArguments();
        return args.isEmpty() ? declaredType : arg0(args.get(0));
      }
      return mirror;
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
    //apply.visitLineNumber(metadata.getLine(), sourceStart);

    /**
     * provider.get()
     */
    apply.visitVarInsn(ALOAD, 0);
    apply.visitFieldInsn(GETFIELD, getHandlerInternal(), PROVIDER_VAR, PROVIDER.getDescriptor());
    apply.visitMethodInsn(INVOKEINTERFACE, PROVIDER.getInternalName(), "get", PROVIDER_DESCRIPTOR,
        true);
    apply.visitTypeInsn(CHECKCAST, owner.getInternalName());

    for (VariableElement parameter : executable.getParameters()) {
      /**
       * Load context
       */
      apply.visitVarInsn(ALOAD, 1);
      ParamType paramType = paramType(parameter);
      String rawType = rawType(paramType.arg0(parameter.asType()));
      if (isTypeInjection(rawType)) {
        if (!isContext(rawType)) {
          Method method = typeInjection(rawType, paramType);
          apply.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), method.getName(),
              Type.getMethodDescriptor(method), true);
        }
        if (paramType.isOptional()) {
          apply.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "ofNullable",
              "(Ljava/lang/Object;)Ljava/util/Optional;", false);
        }
      } else {
        Map.Entry<String, Set<String>> strategy = paramStrategy(parameter);

        String parameterName = parameterName(parameter, strategy.getValue());

        Method paramValue = paramValue(parameter, paramType);
        boolean dynamic =
            paramValue.getName().equals("to") && !isSimpleType(paramType.arg0(parameter.asType()));
        if (dynamic) {
          Method pathParam = Context.class.getDeclaredMethod(strategy.getKey());
          apply.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), pathParam.getName(),
              getMethodDescriptor(pathParam), true);
          apply.visitLdcInsn(asmType(rawType));

          /** to(Reified): */
          paramType.withReified(name -> {
            Method reified = Reified.class.getDeclaredMethod(name, java.lang.reflect.Type.class);
            apply.visitMethodInsn(INVOKESTATIC, "io/jooby/Reified", reified.getName(),
                getMethodDescriptor(reified), false);
          });

          apply.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Value", paramValue.getName(),
              getMethodDescriptor(paramValue), true);
          apply.visitTypeInsn(CHECKCAST, asmType(rawType(parameter)).getInternalName());
        } else {
          apply.visitLdcInsn(parameterName);

          Method pathParam = Context.class.getDeclaredMethod(strategy.getKey(), String.class);
          apply.visitMethodInsn(INVOKEINTERFACE, CTX.getInternalName(), pathParam.getName(),
              getMethodDescriptor(pathParam), true);
          // to(Class)
          boolean toClass = paramValue.getName().equals("to");
          // toOptional(Class) or toList(Class) or toSet(Class)
          boolean toOptColClass =
              (paramType.isOptional() || paramType.isList() || paramType.isSet()) && !eq(
                  String.class, rawType);
          if (toOptColClass || toClass) {
            apply.visitLdcInsn(asmType(rawType));
          }
          apply.visitMethodInsn(INVOKEINTERFACE, "io/jooby/Value", paramValue.getName(),
              getMethodDescriptor(paramValue), true);
          if (toClass) {
            apply.visitTypeInsn(CHECKCAST, asmType(rawType(parameter)).getInternalName());
          }
        }
      }
    }

    /** Invoke. */
    apply.visitMethodInsn(INVOKEVIRTUAL, owner.getInternalName(), methodName, methodDescriptor,
        false);

    apply.visitInsn(ARETURN);

    apply.visitMaxs(0, 0);
    apply.visitEnd();
  }

  private Map.Entry<String, Set<String>> paramStrategy(VariableElement parameter) {
    return PARAMS.entrySet().stream()
        .filter(it -> isParam(parameter, it.getValue()))
        .findFirst()
        .orElseThrow(() -> new ProvisioningException(
            "Unable to provision parameter: '" + parameter + ": " + parameter.asType() + "'",
            null));
  }

  private ParamType paramType(VariableElement parameter) {
    TypeMirror typeMirror = parameter.asType();
    if (isSimpleType(typeMirror)) {
      return ParamType.SIMPLE;
    }
    Types types = environment.getTypeUtils();
    TypeMirror erasure = types.erasure(typeMirror);
    if (eq(Optional.class, erasure)) {
      return ParamType.OPTIONAL;
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
      if (eq(String.class, paramType.arg0(type))) {
        return Value.class.getDeclaredMethod("toOptional");
      } else if (isSimpleType(paramType.arg0(type))) {
        return Value.class.getDeclaredMethod("toOptional", Class.class);
      } else {
        return Value.class.getDeclaredMethod("to", Reified.class);
      }
    } else if (paramType.isList()) {
      if (eq(String.class, paramType.arg0(type))) {
        return Value.class.getDeclaredMethod("toList");
      } else if (isSimpleType(paramType.arg0(type))) {
        return Value.class.getDeclaredMethod("toList", Class.class);
      } else {
        return Value.class.getDeclaredMethod("to", Reified.class);
      }
    } else if (paramType.isSet()) {
      if (eq(String.class, paramType.arg0(type))) {
        return Value.class.getDeclaredMethod("toSet");
      } else if (isSimpleType(paramType.arg0(type))) {
        return Value.class.getDeclaredMethod("toSet", Class.class);
      } else {
        return Value.class.getDeclaredMethod("to", Reified.class);
      }
    } else {
      if (isSimpleType(paramType.arg0(type))) {
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
      case "int":
        return Type.INT_TYPE;
      case "long":
        return Type.LONG_TYPE;
      case "float":
        return Type.FLOAT_TYPE;
      case "double":
        return Type.DOUBLE_TYPE;
      case "boolean":
        return Type.BOOLEAN_TYPE;
      case "void":
        return Type.VOID_TYPE;
      case "short":
        return Type.SHORT_TYPE;
      case "char":
        return Type.CHAR_TYPE;
      default:
        return Type.getObjectType(type.replace(".", "/"));
    }
  }

  private Type[] asmTypes(String... types) {
    return Stream.of(types)
        .map(this::asmType)
        .toArray(Type[]::new);
  }

  private static String providerOf(Type owner) {
    StringBuilder signature = new StringBuilder(PROVIDER.getDescriptor());
    signature.insert(signature.length() - 1, "<" + owner.getDescriptor() + ">");
    return signature.toString();
  }
}
