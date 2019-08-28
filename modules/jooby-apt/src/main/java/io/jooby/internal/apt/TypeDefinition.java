/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import org.objectweb.asm.Type;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.BYTE_TYPE;
import static org.objectweb.asm.Type.CHAR_TYPE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.SHORT_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getObjectType;

public class TypeDefinition {

  private final Types typeUtils;
  private final TypeMirror type;

  public TypeDefinition(Types types, TypeMirror type) {
    this.typeUtils = types;
    this.type = type;
  }

  public String getSimpleName() {
    String name = getName();
    int i = name.lastIndexOf('.');
    return i > 0 ? name.substring(i + 1) : name;
  }

  public String getName() {
    return getRawType().toString();
  }

  public TypeMirror getType() {
    return type;
  }

  public boolean isPrimitive() {
    return getType().getKind().isPrimitive();
  }

  public boolean isVoid() {
    return type.getKind() == TypeKind.VOID;
  }

  public TypeMirror getRawType() {
    return typeUtils.erasure(type);
  }

  public boolean is(Class type, Class... arguments) {
    return is(typeName(type), Stream.of(arguments).map(this::typeName).toArray(String[]::new));
  }

  private boolean is(String type, String... arguments) {
    boolean same = getRawType().toString().equals(type);
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

  public Type toJvmType() {
    return asmType(getName(type));
  }

  public boolean isRawType() {
    return type.toString().equals(getRawType().toString());
  }

  @Override public String toString() {
    return type.toString();
  }

  private org.objectweb.asm.Type asmType(String type) {
    switch (type) {
      case "byte":
        return BYTE_TYPE;
      case "byte[]":
        return org.objectweb.asm.Type.getType(byte[].class);
      case "int":
        return INT_TYPE;
      case "int[]":
        return org.objectweb.asm.Type.getType(int[].class);
      case "long":
        return LONG_TYPE;
      case "long[]":
        return org.objectweb.asm.Type.getType(long[].class);
      case "float":
        return FLOAT_TYPE;
      case "float[]":
        return org.objectweb.asm.Type.getType(float[].class);
      case "double":
        return DOUBLE_TYPE;
      case "double[]":
        return org.objectweb.asm.Type.getType(double[].class);
      case "boolean":
        return BOOLEAN_TYPE;
      case "boolean[]":
        return org.objectweb.asm.Type.getType(boolean[].class);
      case "void":
        return VOID_TYPE;
      case "short":
        return SHORT_TYPE;
      case "short[]":
        return org.objectweb.asm.Type.getType(short[].class);
      case "char":
        return CHAR_TYPE;
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
        return getObjectType(prefix + type.replace(".", "/"));
    }
  }

  private String typeName(Class type) {
    return type.isArray() ? type.getComponentType().getName() + "[]" : type.getName();
  }

  private String getName(TypeMirror type) {
    Element element = typeUtils.asElement(type);
    return element == null ? type.toString() : getName(element);
  }

  private String getName(Element type) {
    Element parent = type.getEnclosingElement();
    if (parent != null && parent.getKind() == ElementKind.CLASS) {
      return getName(parent) + "$" + type.getSimpleName();
    }
    return type.toString();
  }
}
