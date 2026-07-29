/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import io.swagger.v3.oas.models.media.Schema;

/**
 * Makes {@link Schema#getProperties()} order deterministic by reordering keys according to
 * class-file declaration order (fields, then getters), including the superclass chain.
 *
 * <p>This preserves a natural bean-like order
 */
public final class SchemaPropertyOrder {

  private SchemaPropertyOrder() {}

  public static void stabilize(
      ClassNode node, Schema<?> schema, Function<Type, ClassNode> classNodes) {
    Map<String, Schema> properties = schema.getProperties();
    if (properties == null || properties.isEmpty() || node == null) {
      return;
    }
    List<String> declarationOrder = declarationOrder(node, classNodes);
    if (declarationOrder.isEmpty()) {
      return;
    }

    var ordered = new LinkedHashMap<String, Schema>();
    for (String name : declarationOrder) {
      Schema property = properties.get(name);
      if (property != null) {
        ordered.put(name, property);
      }
    }
    // Keep any leftover properties (e.g. synthetic names) in their original relative order.
    properties.forEach(ordered::putIfAbsent);
    schema.setProperties(ordered);
  }

  static List<String> declarationOrder(ClassNode node, Function<Type, ClassNode> classNodes) {
    var names = new LinkedHashSet<String>();
    collect(node, classNodes, names);
    return new ArrayList<>(names);
  }

  private static void collect(
      ClassNode node, Function<Type, ClassNode> classNodes, Set<String> names) {
    if (node == null || isExcluded(node.name)) {
      return;
    }
    if (node.superName != null && !isExcluded(node.superName)) {
      collect(classNodes.apply(Type.getObjectType(node.superName)), classNodes, names);
    }
    if (node.fields != null) {
      for (FieldNode field : node.fields) {
        if (isInstanceField(field)) {
          names.add(field.name);
        }
      }
    }
    if (node.methods != null) {
      for (MethodNode method : node.methods) {
        if (isGetter(method)) {
          names.add(propertyName(method.name));
        }
      }
    }
  }

  private static boolean isExcluded(String internalName) {
    return internalName == null
        || internalName.equals("java/lang/Object")
        || internalName.equals("java/lang/Record")
        || internalName.equals("java/lang/Enum");
  }

  private static boolean isInstanceField(FieldNode field) {
    return (field.access & Opcodes.ACC_STATIC) == 0
        && (field.access & Opcodes.ACC_SYNTHETIC) == 0;
  }

  private static boolean isGetter(MethodNode method) {
    if ((method.access & Opcodes.ACC_STATIC) != 0
        || (method.access & Opcodes.ACC_PUBLIC) == 0
        || (method.access & Opcodes.ACC_SYNTHETIC) != 0
        || (method.access & Opcodes.ACC_BRIDGE) != 0) {
      return false;
    }
    if (Type.getArgumentTypes(method.desc).length != 0) {
      return false;
    }
    Type returnType = Type.getReturnType(method.desc);
    if (returnType.equals(Type.VOID_TYPE)) {
      return false;
    }
    if (method.name.startsWith("get") && method.name.length() > 3) {
      return true;
    }
    return method.name.startsWith("is")
        && method.name.length() > 2
        && (returnType.equals(Type.BOOLEAN_TYPE)
            || returnType.getClassName().equals(Boolean.class.getName()));
  }

  private static String propertyName(String methodName) {
    if (methodName.startsWith("get")) {
      return decapitalize(methodName.substring(3));
    }
    if (methodName.startsWith("is")) {
      return decapitalize(methodName.substring(2));
    }
    return methodName;
  }

  /** Same rules as {@code java.beans.Introspector.decapitalize}. */
  private static String decapitalize(String name) {
    if (name == null || name.isEmpty()) {
      return name;
    }
    if (name.length() > 1
        && Character.isUpperCase(name.charAt(0))
        && Character.isUpperCase(name.charAt(1))) {
      return name;
    }
    char[] chars = name.toCharArray();
    chars[0] = Character.toLowerCase(chars[0]);
    return new String(chars);
  }
}
