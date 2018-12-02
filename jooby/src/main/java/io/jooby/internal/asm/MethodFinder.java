/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
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
      if (debug ) {
        printer = new ASMifier();
        return new TraceMethodVisitor(this.node, printer);
      }
      return this.node;
    }
    return up;
  }
}
