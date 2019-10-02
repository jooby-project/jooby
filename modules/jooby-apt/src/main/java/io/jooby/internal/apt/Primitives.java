/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Method;

public class Primitives {
  public static Method wrapper(TypeDefinition type) throws NoSuchMethodException {
    return wrapper(type.getType());
  }

  public static Method wrapper(TypeMirror type) throws NoSuchMethodException {
    return wrapper(type.getKind());
  }

  public static Method wrapper(TypeKind type) throws NoSuchMethodException {
    return wrapper(type.name().toLowerCase());
  }

  public static Method wrapper(Class type) throws NoSuchMethodException {
    return wrapper(type.getSimpleName());
  }

  public static Method wrapper(String name) throws NoSuchMethodException {
    switch (name.toLowerCase()) {
      case "boolean":
        return Boolean.class.getDeclaredMethod("valueOf", Boolean.TYPE);
      case "char":
      case "character":
        return Character.class.getDeclaredMethod("valueOf", Character.TYPE);
      case "byte":
        return Byte.class.getDeclaredMethod("valueOf", Byte.TYPE);
      case "short":
        return Short.class.getDeclaredMethod("valueOf", Short.TYPE);
      case "int":
      case "integer":
        return Integer.class.getDeclaredMethod("valueOf", Integer.TYPE);
      case "long":
        return Long.class.getDeclaredMethod("valueOf", Long.TYPE);
      case "float":
        return Float.class.getDeclaredMethod("valueOf", Float.TYPE);
      case "double":
        return Double.class.getDeclaredMethod("valueOf", Double.TYPE);
      default:
        return null;
    }
  }

  public static Method toPrimitive(TypeDefinition type) throws NoSuchMethodException {
    return toPrimitive(type.getType());
  }

  public static Method toPrimitive(TypeMirror type) throws NoSuchMethodException {
    return toPrimitive(type.getKind());
  }

  public static Method toPrimitive(TypeKind type) throws NoSuchMethodException {
    return toPrimitive(type.name().toLowerCase());
  }

  public static Method toPrimitive(Class type) throws NoSuchMethodException {
    return toPrimitive(type.getSimpleName());
  }

  public static Method toPrimitive(String name) throws NoSuchMethodException {
    switch (name.toLowerCase()) {
      case "boolean":
        return Boolean.class.getDeclaredMethod("booleanValue");
      case "character":
        return Character.class.getDeclaredMethod("charValue");
      case "byte":
        return Byte.class.getDeclaredMethod("byteValue");
      case "short":
        return Short.class.getDeclaredMethod("shortValue");
      case "int":
      case "integer":
        return Integer.class.getDeclaredMethod("intValue");
      case "long":
        return Long.class.getDeclaredMethod("longValue");
      case "float":
        return Float.class.getDeclaredMethod("floatValue");
      case "double":
        return Double.class.getDeclaredMethod("doubleValue");
      default:
        return null;
    }
  }
}
