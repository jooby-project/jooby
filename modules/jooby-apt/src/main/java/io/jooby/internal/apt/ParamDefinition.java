/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import io.jooby.internal.apt.MethodDescriptor.Value;
import io.jooby.internal.apt.MethodDescriptor.ValueNode;
import io.jooby.internal.apt.asm.ParamWriter;

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

  public boolean is(String type, String... arguments) {
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
    if (hasAnnotation(".Nullable") || hasAnnotation("edu.umd.cs.findbugs.annotations.Nullable")) {
      return true;
    }
    boolean nonnull =
        hasAnnotation(".NotNull")
            || hasAnnotation(".NonNull")
            || hasAnnotation("edu.umd.cs.findbugs.annotations.NonNull");
    if (nonnull) {
      return false;
    }
    return !getType().isPrimitive();
  }

  private boolean hasAnnotation(String type) {
    Set<String> annotations = annotations(parameter);
    for (String annotation : annotations) {
      if (annotation.endsWith(type)) {
        return true;
      }
    }
    return false;
  }

  private Set<String> annotations(VariableElement parameter) {
    Set<String> annotations = new LinkedHashSet<>();
    annotations.addAll(annotationsFrom(parameter));
    annotations.addAll(annotationsFrom(parameter.asType()));
    return annotations;
  }

  private Set<String> annotationsFrom(AnnotatedConstruct annotated) {
    Set<String> annotations = new LinkedHashSet<>();
    for (AnnotationMirror annotation : annotated.getAnnotationMirrors()) {
      TypeMirror typeMirror = annotation.getAnnotationType();
      annotations.add(typeMirror.toString());
    }
    return annotations;
  }

  public MethodDescriptor getObjectValue() throws NoSuchMethodException {
    return getKind().valueObject(this);
  }

  public MethodDescriptor getSingleValue() throws NoSuchMethodException {
    return getKind().singleValue(this);
  }

  public boolean isSimpleType() {
    for (String builtinType : builtinTypes()) {
      if (is(builtinType)
          || is(Optional.class.getName(), builtinType)
          || is(List.class.getName(), builtinType)
          || is(Set.class.getName(), builtinType)) {
        return true;
      }
    }
    return false;
  }

  public String[] sources() {
    return annotations(parameter.getAnnotationMirrors(), this.kind.annotations()).stream()
        .flatMap(
            it ->
                Annotations.attribute(
                    it, "value", v -> ((VariableElement) v.getValue()).getSimpleName().toString())
                    .stream())
        .toArray(String[]::new);
  }

  private String[] builtinTypes() {
    return new String[] {
      String.class.getName(),
      Boolean.class.getName(),
      Boolean.TYPE.getName(),
      Byte.class.getName(),
      Byte.TYPE.getName(),
      Character.class.getName(),
      Character.TYPE.getName(),
      Short.class.getName(),
      Short.TYPE.getName(),
      Integer.class.getName(),
      Integer.TYPE.getName(),
      Long.class.getName(),
      Long.TYPE.getName(),
      Float.class.getName(),
      Float.TYPE.getName(),
      Double.class.getName(),
      Double.TYPE.getName(),
      Enum.class.getName(),
      java.util.UUID.class.getName(),
      java.time.Instant.class.getName(),
      java.util.Date.class.getName(),
      java.time.LocalDate.class.getName(),
      java.time.LocalDateTime.class.getName(),
      java.math.BigDecimal.class.getName(),
      java.math.BigInteger.class.getName(),
      Duration.class.getName(),
      Period.class.getName(),
      java.nio.charset.Charset.class.getName(),
      JoobyTypes.StatusCode.getClassName(),
      TimeZone.class.getName(),
      ZoneId.class.getName(),
      URI.class.getName(),
      URL.class.getName()
    };
  }

  @Override
  public String toString() {
    return parameter.getSimpleName() + ": " + parameter.asType();
  }

  public MethodDescriptor getMethod() throws NoSuchMethodException {
    if (!isNullable()) {
      if (is(String.class)) {
        return Value.value();
      }
      if (is(int.class)) {
        return Value.intValue();
      }
      if (is(byte.class)) {
        return Value.byteValue();
      }
      if (is(long.class)) {
        return Value.longValue();
      }
      if (is(float.class)) {
        return Value.floatValue();
      }
      if (is(double.class)) {
        return Value.doubleValue();
      }
      if (is(boolean.class)) {
        return Value.booleanValue();
      }
      if (is(Optional.class, String.class)) {
        return Value.toOptional();
      }
      if (is(List.class, String.class)) {
        return Value.toList();
      }
      if (is(Set.class, String.class)) {
        return Value.toSet();
      }
    }
    // toOptional(Class)
    if (isOptional()) {
      return ValueNode.toOptional();
    }
    if (isList()) {
      return ValueNode.toList();
    }
    if (is(Set.class)) {
      return ValueNode.toSet();
    }
    if (kind == ParamKind.BODY_PARAM) {
      return type.isRawType()
          ? MethodDescriptor.Context.bodyClass()
          : MethodDescriptor.Context.bodyType();
    }
    return ValueNode.to();
  }

  public static ParamDefinition create(
      ProcessingEnvironment environment, VariableElement parameter) {
    ParamDefinition definition = new ParamDefinition(environment, parameter);
    return definition;
  }

  private ParamKind computeKind() {
    if (isTypeInjection()) {
      return ParamKind.TYPE;
    }

    if (is(JoobyTypes.FileUpload.getClassName())
        || is(List.class.getName(), JoobyTypes.FileUpload.getClassName())
        || is(Optional.class.getName(), JoobyTypes.FileUpload.getClassName())
        || is(Path.class)) {
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
    if (is(JoobyTypes.Context.getClassName())) {
      return true;
    }
    if (is(JoobyTypes.QueryString.getClassName())) {
      return true;
    }
    if (is(JoobyTypes.Formdata.getClassName())) {
      return true;
    }
    if (is(JoobyTypes.FlashMap.getClassName())) {
      return true;
    }
    if (is(JoobyTypes.Session.getClassName())
        || is(Optional.class.getName(), JoobyTypes.Session.getClassName())) {
      return true;
    }
    return is(JoobyTypes.Route.getClassName());
  }

  private boolean isParam(VariableElement parameter, Set<String> annotations) {
    return annotations(parameter.getAnnotationMirrors(), annotations).size() > 0;
  }

  private List<AnnotationMirror> annotations(
      List<? extends AnnotationMirror> annotationMirrors, Set<String> annotations) {
    return annotationMirrors.stream()
        .filter(
            it -> {
              String rawType =
                  new TypeDefinition(typeUtils, it.getAnnotationType()).getRawType().toString();
              return annotations.contains(rawType);
            })
        .collect(Collectors.toList());
  }

  private String parameterName(VariableElement parameter, Set<String> types) {
    return annotations(parameter.getAnnotationMirrors(), types).stream()
        .flatMap(it -> Annotations.attribute(it, kind.httpNameMemberName()).stream())
        .findFirst()
        .orElse(parameter.getSimpleName().toString());
  }
}
