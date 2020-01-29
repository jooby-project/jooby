package io.jooby.internal.openapi;

import io.jooby.Router;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.MethodInsnNode;

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

  public boolean isKoobyInit() {
    return matches("<init>", TypeFactory.KT_FUN_1);
  }

  public boolean isRouteHandler() {
    return matches(TypeFactory.STRING, TypeFactory.HANDLER) || matches(TypeFactory.STRING,
        TypeFactory.KT_FUN_1);
  }

  public boolean matches(String method, Type... parameterTypes) {
    if (this.method.equals(method)) {
      return matches(parameterTypes);
    }
    return false;
  }

  public boolean matches(String method, Class... parameterTypes) {
    if (this.method.equals(method)) {
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
    return owner.getDescriptor();
  }

  public static Signature create(MethodInsnNode node) {
    return new Signature(TypeFactory.fromInternalName(node.owner), node.name, node.desc);
  }
}
