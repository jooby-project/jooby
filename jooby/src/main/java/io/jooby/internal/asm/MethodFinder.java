/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.lang.reflect.Method;

public class MethodFinder extends ClassVisitor {
  private final String descriptor;
  private final boolean debug;
  private final String name;
  public MethodNode node;
  public ASMifier printer;

  public MethodFinder(Method method, boolean debug) {
    super(Opcodes.ASM6);
    this.descriptor = Type.getMethodDescriptor(method);
    this.name = method.getName();
    this.debug = debug;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
      String[] exceptions) {
    MethodVisitor up = super.visitMethod(access, name, descriptor, signature, exceptions);
    if (this.name.equals(name) && this.descriptor.equals(descriptor)) {
      this.node = new MethodNode(access, name, descriptor, signature, exceptions);
      if (debug) {
        if (printer == null) {
          printer = new ASMifier();
        }
        return new TraceMethodVisitor(this.node, printer);
      }
      return this.node;
    } else {
      if (debug) {
        if (printer == null) {
          printer = new ASMifier();
        }
        return new TraceMethodVisitor(this.node, printer);
      }
    }
    return up;
  }
}
