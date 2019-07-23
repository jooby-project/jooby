package io.jooby.internal.compiler;

import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Reified;
import io.jooby.Session;
import io.jooby.Value;
import org.objectweb.asm.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParamDefinition {

  private final VariableElement parameter;
  private final TypeMirror type;
  private final TypeMirror rawType;
  private final Types typeUtils;
  private final ParamKind kind;
  private final String name;
  private final String httpName;

  private ParamDefinition(ProcessingEnvironment environment, VariableElement parameter) {
    this.typeUtils = environment.getTypeUtils();
    this.parameter = parameter;
    this.name = parameter.getSimpleName().toString();
    this.type = parameter.asType();
    this.rawType = environment.getTypeUtils().erasure(type);
    this.kind = computeKind();
    this.httpName = parameterName(parameter, this.kind.annotations());
  }

  public ParamWriter newWriter() {
    try {
      return getKind().newWriter();
    } catch (UnsupportedOperationException x) {
      throw new UnsupportedOperationException(
          "No writer for: '" + toString() + "'; kind: " + getKind());
    }
  }

  public String getHttpName() {
    return httpName;
  }

  public String getName() {
    return name;
  }

  public TypeMirror getType() {
    return type;
  }

  public TypeMirror getRawType() {
    return rawType;
  }

  public TypeMirror getTypeArgument(int index) {
    if (type instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) type;
      List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
      if (index < typeArguments.size()) {
        return typeUtils.erasure(typeArguments.get(index));
      }
    }
    throw new NoSuchElementException("No generic type: " + type);
  }

  public ParamKind getKind() {
    return kind;
  }

  public boolean is(Class type, Class... arguments) {
    return is(typeName(type), Stream.of(arguments).map(this::typeName).toArray(String[]::new));
  }

  public boolean is(String type, String... arguments) {
    boolean same = rawType.toString().equals(type);
    if (!same) {
      return false;
    }
    if (arguments.length > 0 && this.type instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) this.type;
      List<? extends TypeMirror> args = declaredType.getTypeArguments();
      if (args.size() != arguments.length) {
        return false;
      }
      for (int i = 0; i < arguments.length; i++) {
        if (!arguments[i].equals(typeUtils.erasure(args.get(i)).toString())) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean isOptional() {
    return is(Optional.class);
  }

  public boolean isList() {
    return is(List.class);
  }

  public boolean isNamed() {
    return isSimpleType();
  }

  public Method getObjectValue() throws NoSuchMethodException {
    return getKind().valueObject(this);
  }

  public Method getSingleValue() throws NoSuchMethodException {
    return getKind().singleValue(this);
  }

  private String typeName(Class type) {
    return type.isArray() ? type.getComponentType().getName() + "[]" : type.getName();
  }

  private boolean isSimpleType() {
    for (Class builtinType : builtinTypes()) {
      if (is(builtinType) || is(Optional.class, builtinType) || is(List.class, builtinType) || is(
          Set.class, builtinType)) {
        return true;
      }
    }
    return false;
  }

  private Class[] builtinTypes() {
    return new Class[]{
        Boolean.class,
        Boolean.TYPE,
        Byte.class,
        Byte.TYPE,
        Character.class,
        Character.TYPE,
        Short.class,
        Short.TYPE,
        Integer.class,
        Integer.TYPE,
        Long.class,
        Long.TYPE,
        Float.class,
        Float.TYPE,
        Double.class,
        Double.TYPE,
        String.class,
        java.time.Instant.class,
        java.util.UUID.class,
        java.math.BigDecimal.class,
        java.math.BigInteger.class,
        java.nio.charset.Charset.class,
        io.jooby.StatusCode.class
    };
  }

  public org.objectweb.asm.Type getByteCodeType() {
    return asmType(getRawType().toString());
  }

  public org.objectweb.asm.Type getByteCodeTypeArgument(int index) {
    return asmType(getTypeArgument(index).toString());
  }

  public org.objectweb.asm.Type[] getByteCodeTypeArguments() {
    if (type instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) type;
      List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
      Type[] result = new Type[typeArguments.size()];
      for (int i = 0; i < result.length; i++) {
        result[i] = asmType(typeUtils.erasure(typeArguments.get(i)).toString());
      }
      return result;
    }
    return new Type[0];
  }

  public boolean isGenericType() {
    if (type instanceof DeclaredType) {
      return ((DeclaredType) type).getTypeArguments().size() > 0;
    }
    return false;
  }

  @Override public String toString() {
    return parameter.getSimpleName() + ": " + parameter.asType();
  }

  private org.objectweb.asm.Type asmType(String type) {
    switch (type) {
      case "byte":
        return org.objectweb.asm.Type.BYTE_TYPE;
      case "byte[]":
        return org.objectweb.asm.Type.getType(byte[].class);
      case "int":
        return org.objectweb.asm.Type.INT_TYPE;
      case "int[]":
        return org.objectweb.asm.Type.getType(int[].class);
      case "long":
        return org.objectweb.asm.Type.LONG_TYPE;
      case "long[]":
        return org.objectweb.asm.Type.getType(long[].class);
      case "float":
        return org.objectweb.asm.Type.FLOAT_TYPE;
      case "float[]":
        return org.objectweb.asm.Type.getType(float[].class);
      case "double":
        return org.objectweb.asm.Type.DOUBLE_TYPE;
      case "double[]":
        return org.objectweb.asm.Type.getType(double[].class);
      case "boolean":
        return org.objectweb.asm.Type.BOOLEAN_TYPE;
      case "boolean[]":
        return org.objectweb.asm.Type.getType(boolean[].class);
      case "void":
        return org.objectweb.asm.Type.VOID_TYPE;
      case "short":
        return org.objectweb.asm.Type.SHORT_TYPE;
      case "short[]":
        return org.objectweb.asm.Type.getType(short[].class);
      case "char":
        return org.objectweb.asm.Type.CHAR_TYPE;
      case "char[]":
        return org.objectweb.asm.Type.getType(char[].class);
      case "String":
        return org.objectweb.asm.Type.getType(String.class);
      case "String[]":
        return org.objectweb.asm.Type.getType(String[].class);
      default:
        String prefix = "";
        if (type.endsWith("[]")) {
          prefix = "[";
        }
        return org.objectweb.asm.Type.getObjectType(prefix + type.replace(".", "/"));
    }
  }

  public Method getMethod() throws NoSuchMethodException {
    if (is(String.class)) {
      return Value.class.getDeclaredMethod("value");
    }
    if (is(int.class)) {
      return Value.class.getDeclaredMethod("intValue");
    }
    if (is(byte.class)) {
      return Value.class.getDeclaredMethod("byteValue");
    }
    if (is(long.class)) {
      return Value.class.getDeclaredMethod("longValue");
    }
    if (is(float.class)) {
      return Value.class.getDeclaredMethod("floatValue");
    }
    if (is(double.class)) {
      return Value.class.getDeclaredMethod("doubleValue");
    }
    if (is(boolean.class)) {
      return Value.class.getDeclaredMethod("booleanValue");
    }
    if (is(Optional.class, String.class)) {
      return Value.class.getDeclaredMethod("toOptional");
    }
    if (is(List.class, String.class)) {
      return Value.class.getDeclaredMethod("toList");
    }
    if (is(Set.class, String.class)) {
      return Value.class.getDeclaredMethod("toSet");
    }
    // toOptional(Class)
    if (isOptional()) {
      return Value.class.getMethod("toOptional", Class.class);
    }
    if (isList()) {
      return Value.class.getMethod("toList", Class.class);
    }
    if (is(Set.class)) {
      return Value.class.getMethod("toSet", Class.class);
    }
    boolean useClass = getType().toString().equals(getRawType().toString());
    if (useClass) {
      return Value.class.getMethod("to", Class.class);
    }
    return Value.class.getMethod("to", Reified.class);
  }

  public static ParamDefinition create(ProcessingEnvironment environment,
      VariableElement parameter) {
    ParamDefinition definition = new ParamDefinition(environment, parameter);
    return definition;
  }

  private ParamKind computeKind() {
    if (isTypeInjection()) {
      return ParamKind.TYPE;
    }

    if (is(FileUpload.class) ||
        is(List.class, FileUpload.class) ||
        is(Optional.class, FileUpload.class) ||
        is(Path.class)) {
      return ParamKind.FILE_UPLOAD;
    }

    for (ParamKind strategy : ParamKind.values()) {
      if (isParam(parameter, strategy.annotations())) {
        return strategy;
      }
    }

    return ParamKind.BODY_PARAM;
  }

  private boolean isTypeInjection() {
    if (is(Context.class)) {
      return true;
    }
    if (is(QueryString.class)) {
      return true;
    }
    if (is(Formdata.class)) {
      return true;
    }
    if (is(Multipart.class)) {
      return true;
    }
    if (is(FlashMap.class)) {
      return true;
    }
    if (is(Session.class) || is(Optional.class, Session.class)) {
      return true;
    }
    return false;
  }

  private boolean isParam(VariableElement parameter, Set<String> annotations) {
    return annotations(parameter.getAnnotationMirrors(), annotations).size() > 0;
  }

  private List<AnnotationMirror> annotations(List<? extends AnnotationMirror> annotationMirrors,
      Set<String> annotations) {
    return annotationMirrors.stream()
        .filter(it -> {
          String rawType = typeUtils.erasure(it.getAnnotationType()).toString();
          return annotations.contains(rawType);
        })
        .collect(Collectors.toList());
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
}
