/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class Signature {

  private final Type[] argumentTypes;

  private Type owner;

  private String method;

  private String descriptor;

  public Signature(Type owner, String method, String descriptor) {
    this.owner = owner;
    this.method = method;
    this.descriptor = descriptor;
    this.argumentTypes = Type.getArgumentTypes(descriptor);
  }

  public Optional<Type> getOwner() {
    return Optional.ofNullable(owner);
  }

  public String getMethod() {
    return method;
  }

  public String getDescriptor() {
    return descriptor;
  }

  public boolean matches(String method) {
    return this.method.equals(method);
  }

  public boolean matches(String method, Type... parameterTypes) {
    if (matches(method)) {
      return matches(parameterTypes);
    }
    return false;
  }

  public boolean matches(Class owner, String method, Class... parameterTypes) {
    if (Type.getType(owner).equals(this.owner)) {
      return matches(method, parameterTypes);
    }
    return false;
  }

  public boolean matches(Type owner, String method) {
    if (owner.equals(this.owner)) {
      return matches(method);
    }
    return false;
  }

  public boolean matches(String method, Class... parameterTypes) {
    if (matches(method)) {
      return matches(parameterTypes);
    }
    return false;
  }

  public boolean matches(Class... parameterTypes) {
    return matches(Stream.of(parameterTypes).map(Type::getType).toArray(Type[]::new));
  }

  public boolean matches(Type... parameterTypes) {
    return Arrays.equals(this.argumentTypes, parameterTypes);
  }

  public int getParameterCount() {
    return argumentTypes.length;
  }

  @Override public String toString() {
    return getMethod() + getDescriptor();
  }

  public static Signature create(MethodInsnNode node) {
    return new Signature(TypeFactory.fromInternalName(node.owner), node.name, node.desc);
  }

  public static Signature create(MethodNode node) {
    return new Signature(null, node.name, node.desc);
  }
}
