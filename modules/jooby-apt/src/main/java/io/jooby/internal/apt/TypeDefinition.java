/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.CodeBlock.clazz;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class TypeDefinition {
  private final Types typeUtils;
  private final TypeMirror type;
  private final TypeMirror unwrapType;
  private final TypeMirror rawType;
  private final boolean projection;

  public TypeDefinition(Types types, TypeMirror type) {
    this(types, type, false);
  }

  public TypeDefinition(Types types, TypeMirror type, boolean projection) {
    this.typeUtils = types;
    this.type = type;
    this.unwrapType = unwrapType(type);
    this.rawType = typeUtils.erasure(unwrapType);
    this.projection = projection;
  }

  public boolean isProjection() {
    return projection;
  }

  public String toSourceCode(boolean kt) {
    return toSourceCode(this, kt, false);
  }

  public String getArgumentsString(boolean kt, boolean convertTypeVar, Set<TypeKind> kinds) {
    List<TypeDefinition> arguments = getArguments();
    var filtered =
        arguments.stream()
            .filter(it -> kinds.isEmpty() || kinds.contains(it.getType().getKind()))
            .toList();
    if (filtered.isEmpty()) {
      return "";
    }
    return filtered.stream()
        .map(
            it ->
                kt
                    ? convertTypeVar && it.getType().getKind() == TypeKind.TYPEVAR
                        ? "Any"
                        : CodeBlock.type(kt, it.toString())
                    : CodeBlock.type(kt, it.toString()))
        .collect(Collectors.joining(", ", "<", "> "));
  }

  private static String toSourceCode(TypeDefinition type, boolean kt, boolean generics) {
    if (type.isParameterizedType()) {
      var buffer = new StringBuilder();
      var methodName =
          switch (type.getRawType().toString()) {
            case "java.util.Optional" -> "optional";
            case "java.util.List" ->
                "list" + (kt ? type.getArgumentsString(kt, true, Set.of()).trim() : "");
            case "java.util.Set" ->
                "set" + (kt ? type.getArgumentsString(kt, true, Set.of()).trim() : "");
            case "java.util.Map" ->
                "map" + (kt ? type.getArgumentsString(kt, true, Set.of()).trim() : "");
            default -> "getParameterized";
          };
      buffer.append("io.jooby.Reified.").append(methodName).append("(");
      var separator = ", ";
      if (methodName.equals("getParameterized")) {
        // Add raw type
        buffer
            .append(CodeBlock.type(kt, type.getRawType().toString()))
            .append(clazz(kt))
            .append(", ");
      }
      for (TypeDefinition arg : type.getArguments()) {
        // Generics are always wrapper
        buffer.append(toSourceCode(arg, kt, true)).append(separator);
      }
      buffer.setLength(buffer.length() - separator.length());
      buffer.append(").getType()");
      return buffer.toString();
    } else {
      if (generics && kt) {
        return type.getRawType().toString().replace("java.lang.", "") + clazz(kt);
      }
      return CodeBlock.type(kt, type.getRawType().toString()) + clazz(kt);
    }
  }

  /**
   * Check for declared type and get the underlying type. This is required for annotated type.
   * Example:
   *
   * <pre>{@code
   * @Nullable @QueryParam String name
   * }</pre>
   *
   * @param type Type top check
   * @return TypeMirror
   */
  private static TypeMirror unwrapType(TypeMirror type) {
    return (type instanceof DeclaredType) ? ((DeclaredType) type).asElement().asType() : type;
  }

  public String getName() {
    return getRawType().toString();
  }

  public TypeMirror getType() {
    return type;
  }

  public TypeMirror getUnwrapType() {
    return unwrapType;
  }

  public boolean isPrimitive() {
    return unwrapType.getKind().isPrimitive();
  }

  public boolean isVoid() {
    return unwrapType.getKind() == TypeKind.VOID;
  }

  public TypeMirror getRawType() {
    return rawType;
  }

  public boolean is(Class type, Class... arguments) {
    return is(typeName(type), Stream.of(arguments).map(this::typeName).toArray(String[]::new));
  }

  public boolean is(String type, String... arguments) {
    if (equals(getType(), type)) {
      return false;
    }
    if (arguments.length > 0 && this.type instanceof DeclaredType declaredType) {
      List<? extends TypeMirror> args = declaredType.getTypeArguments();
      if (args.size() != arguments.length) {
        return false;
      }
      for (int i = 0; i < arguments.length; i++) {
        if (equals(args.get(i), arguments[i])) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean equals(TypeMirror type, String typeName) {
    var realType = unwrapType(type);
    var erasure = typeUtils.erasure(realType);
    if (!erasure.toString().equals(typeName)) {
      // check for enum subclasses:
      if (Enum.class.getName().equals(typeName)) {
        var element = typeUtils.asElement(realType);
        return element == null || element.getKind() != ElementKind.ENUM;
      } else {
        return true;
      }
    }
    return false;
  }

  public boolean isParameterizedType() {
    if (type instanceof DeclaredType declaredType) {
      return !declaredType.getTypeArguments().isEmpty();
    }
    return false;
  }

  public List<TypeDefinition> getArguments() {
    if (type instanceof DeclaredType declaredType) {
      List<TypeDefinition> result = new ArrayList<>();
      for (TypeMirror typeArgument : declaredType.getTypeArguments()) {
        result.add(new TypeDefinition(typeUtils, typeArgument));
      }
      return result;
    }
    return Collections.emptyList();
  }

  @Override
  public String toString() {
    return type.toString();
  }

  private String typeName(Class type) {
    return type.isArray() ? type.getComponentType().getName() + "[]" : type.getName();
  }
}
