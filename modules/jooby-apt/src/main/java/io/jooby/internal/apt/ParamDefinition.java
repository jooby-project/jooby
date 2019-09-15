/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.Session;
import io.jooby.Value;
import io.jooby.apt.Annotations;
import io.jooby.internal.ValueConverters;
import io.jooby.internal.apt.asm.ParamWriter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ParamDefinition {

  private final VariableElement parameter;
  private final TypeDefinition type;
  private final Types typeUtils;
  private final ParamKind kind;
  private final String name;
  private final String httpName;

  private ParamDefinition(ProcessingEnvironment environment, VariableElement parameter) {
    this.typeUtils = environment.getTypeUtils();
    this.parameter = parameter;
    this.name = parameter.getSimpleName().toString();
    this.type = new TypeDefinition(typeUtils, parameter.asType());
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

  public TypeDefinition getType() {
    return type;
  }

  public ParamKind getKind() {
    return kind;
  }

  public boolean is(Class type, Class... arguments) {
    return getType().is(type, arguments);
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

  public boolean isNullable() {
    if (hasAnnotation("org.jetbrains.annotations.Nullable")
        || hasAnnotation("javax.annotation.Nullable")) {
      return true;
    }
    boolean nonnull = hasAnnotation("org.jetbrains.annotations.NotNull")
        || hasAnnotation("javax.annotation.Nonnull");
    if (nonnull) {
      return false;
    }
    return !getType().isPrimitive();
  }

  private boolean hasAnnotation(String type) {
    for (AnnotationMirror annotation : parameter.getAnnotationMirrors()) {
      if (annotation.getAnnotationType().equals(type)) {
        return true;
      }
    }
    return false;
  }

  public Method getObjectValue() throws NoSuchMethodException {
    return getKind().valueObject(this);
  }

  public Method getSingleValue() throws NoSuchMethodException {
    return getKind().singleValue(this);
  }

  private boolean isSimpleType() {
    for (Class builtinType : builtinTypes()) {
      if (is(builtinType) || is(Optional.class, builtinType) || is(List.class, builtinType) || is(
          Set.class, builtinType)) {
        return true;
      }
    }
    ElementKind kind = typeUtils.asElement(type.getType()).getKind();
    if (kind == ElementKind.ENUM
        || ((is(Optional.class) || is(List.class) || is(Set.class))) && kind == ElementKind.ENUM) {
      return true;
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
        Enum.class,
        java.time.Instant.class,
        java.util.UUID.class,
        java.math.BigDecimal.class,
        java.math.BigInteger.class,
        java.nio.charset.Charset.class,
        io.jooby.StatusCode.class
    };
  }

  @Override public String toString() {
    return parameter.getSimpleName() + ": " + parameter.asType();
  }

  public Method getMethod() throws NoSuchMethodException {
    if (!isNullable()) {
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
    boolean body = kind == ParamKind.BODY_PARAM;
    boolean useClass = type.isRawType();
    if (useClass) {
      return body
          ? Context.class.getMethod("body", Class.class)
          : Value.class.getMethod("to", Class.class);
    }
    return body
        ? Context.class.getMethod("body", Type.class)
        : Value.class.getMethod("to", Class.class);
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
    if (is(Route.class)) {
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
          String rawType = new TypeDefinition(typeUtils, it.getAnnotationType())
              .getRawType()
              .toString();
          return annotations.contains(rawType);
        })
        .collect(Collectors.toList());
  }

  private String parameterName(VariableElement parameter, Set<String> types) {
    return annotations(parameter.getAnnotationMirrors(), types).stream()
        .flatMap(it -> Annotations.attribute(it, "value").stream())
        .findFirst()
        .orElse(parameter.getSimpleName().toString());
  }

}
