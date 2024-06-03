/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.newapt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class TypeDefinition {

  private final Types typeUtils;
  private final TypeMirror type;

  public TypeDefinition(Types types, TypeMirror type) {
    this.typeUtils = types;
    this.type = type;
  }

  public String toSourceCode() {
    return toSourceCode(this);
  }

  private static String toSourceCode(TypeDefinition type) {
    if (type.isParameterizedType()) {
      var buffer = new StringBuilder();
      var methodName =
          switch (type.getRawType().toString()) {
            case "java.util.Optional" -> "optional";
            case "java.util.List" -> "list";
            case "java.util.Set" -> "set";
            case "java.util.Map" -> "map";
            default -> "getParameterized";
          };
      buffer.append("io.jooby.Reified.").append(methodName).append("(");
      var separator = ", ";
      if (methodName.equals("getParameterized")) {
        // Add raw type
        buffer.append(type.getRawType().toString()).append(".class, ");
      }
      for (TypeDefinition arg : type.getArguments()) {
        buffer.append(toSourceCode(arg)).append(separator);
      }
      buffer.setLength(buffer.length() - separator.length());
      buffer.append(").getType()");
      return buffer.toString();
    } else {
      return type.getRawType().toString() + ".class";
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
   * @param type
   * @return
   */
  private TypeMirror unwrapType(TypeMirror type) {
    if (type instanceof DeclaredType) {
      return ((DeclaredType) type).asElement().asType();
    } else {
      return type;
    }
  }

  public String getName() {
    return getRawType().toString();
  }

  public TypeMirror getType() {
    return type;
  }

  public boolean isPrimitive() {
    return unwrapType(getType()).getKind().isPrimitive();
  }

  public boolean isVoid() {
    return unwrapType(getType()).getKind() == TypeKind.VOID;
  }

  public TypeMirror getRawType() {
    return typeUtils.erasure(unwrapType(getType()));
  }

  public boolean is(Class type, Class... arguments) {
    return is(typeName(type), Stream.of(arguments).map(this::typeName).toArray(String[]::new));
  }

  public boolean is(String type, String... arguments) {
    if (!equalType(getType(), type)) {
      return false;
    }
    if (arguments.length > 0 && this.type instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) this.type;
      List<? extends TypeMirror> args = declaredType.getTypeArguments();
      if (args.size() != arguments.length) {
        return false;
      }
      for (int i = 0; i < arguments.length; i++) {
        if (!equalType(args.get(i), arguments[i])) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean equalType(TypeMirror type, String typeName) {
    TypeMirror realType = unwrapType(type);
    TypeMirror erasure = typeUtils.erasure(realType);
    if (!erasure.toString().equals(typeName)) {
      // check for enum subclasses:
      if (Enum.class.getName().equals(typeName)) {
        var element = typeUtils.asElement(realType);
        return element != null && element.getKind() == ElementKind.ENUM;
      } else {
        return false;
      }
    }
    return true;
  }

  public boolean isParameterizedType() {
    if (type instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) type;
      return declaredType.getTypeArguments().size() > 0;
    }
    return false;
  }

  public List<TypeDefinition> getArguments() {
    if (type instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) type;
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
