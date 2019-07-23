package io.jooby.compiler;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.internal.compiler.ParamDefinition;
import io.jooby.internal.compiler.ParamWriter;
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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
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
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
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

    /**
     * provider.get()
     */
    apply.visitVarInsn(ALOAD, 0);
    apply.visitFieldInsn(GETFIELD, getHandlerInternal(), PROVIDER_VAR, PROVIDER.getDescriptor());
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
      writer.accept(classWriter, getHandlerInternal(), visitor, param);
    }
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
        case BOOLEAN:
          wrapper = Boolean.class.getDeclaredMethod("valueOf", Boolean.TYPE);
          break;
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

  private boolean eq(Class type, String typeString) {
    return type.getName().equals(typeString);
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

  private static String providerOf(Type owner) {
    StringBuilder signature = new StringBuilder(PROVIDER.getDescriptor());
    signature.insert(signature.length() - 1, "<" + owner.getDescriptor() + ">");
    return signature.toString();
  }
}
