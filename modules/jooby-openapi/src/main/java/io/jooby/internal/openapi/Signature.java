package io.jooby.internal.openapi;

import io.jooby.Router;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Signature {

  private Type owner;

  private String method;

  private String descriptor;

  public Signature(Type owner, String method, String descriptor) {
    this.owner = owner;
    this.method = method;
    this.descriptor = descriptor;
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
    AtomicInteger i = new AtomicInteger();
    SignatureVisitor visitor = new SignatureVisitor(Opcodes.ASM7) {
      @Override public void visitClassType(String name) {
        if (i.get() < parameterTypes.length) {
          Type type = parameterTypes[i.get()];
          String internalName = type.getInternalName();
          if (internalName.equals(name)) {
            i.incrementAndGet();
          }
        }
      }
    };
    SignatureReader reader = new SignatureReader(descriptor);
    reader.accept(visitor);
    return i.get() == parameterTypes.length;
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
