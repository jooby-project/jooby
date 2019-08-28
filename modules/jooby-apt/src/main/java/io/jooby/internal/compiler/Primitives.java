/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.compiler;

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
}
