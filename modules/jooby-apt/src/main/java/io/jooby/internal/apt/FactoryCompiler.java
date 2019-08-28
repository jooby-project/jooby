/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import io.jooby.MvcFactory;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.objectweb.asm.Type.getType;

public class FactoryCompiler {
  private static final Type OBJ = getType(Object.class);
  private static final Type MVC_EXTENSION = getType(MvcFactory.class);

  private final String moduleFactoryClass;
  private final String moduleFactoryInternalName;
  private final String moduleFactoryJava;
  private final String controllerClass;
  private final String moduleClass;

  public FactoryCompiler(String controllerClass, String moduleClass) {
    this.controllerClass = controllerClass;
    this.moduleClass = moduleClass;
    this.moduleFactoryClass = controllerClass + "$Factory";
    this.moduleFactoryJava = this.moduleFactoryClass + ".java";
    this.moduleFactoryInternalName = this.moduleFactoryClass.replace(".", "/");
  }

  public String getModuleFactoryClass() {
    return moduleFactoryClass;
  }

  public byte[] compile() throws Exception {
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    // public class Controller$methodName implements Route.Handler {
    writer.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, moduleFactoryInternalName, null,
        OBJ.getInternalName(),
        new String[]{MVC_EXTENSION.getInternalName()});
    writer.visitSource(moduleFactoryJava, null);

    init(writer);

    supports(writer);

    create(writer);

    writer.visitEnd();

    return writer.toByteArray();
  }

  private void init(ClassWriter writer) {
    MethodVisitor visitor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    visitor.visitCode();
    visitor.visitVarInsn(ALOAD, 0);
    visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    visitor.visitInsn(Opcodes.RETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private void create(ClassWriter writer) {
    MethodVisitor visitor = writer
        .visitMethod(ACC_PUBLIC, "create", "(Ljavax/inject/Provider;)Lio/jooby/Extension;", null,
            null);
    visitor.visitParameter("provider", 0);
    visitor.visitCode();
    visitor.visitTypeInsn(NEW, moduleClass.replace(".", "/"));
    visitor.visitInsn(DUP);
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitMethodInsn(INVOKESPECIAL, moduleClass.replace(".", "/"), "<init>",
        "(Ljavax/inject/Provider;)V", false);
    visitor.visitInsn(ARETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private void supports(ClassWriter writer) {
    MethodVisitor visitor = writer
        .visitMethod(ACC_PUBLIC, "supports", "(Ljava/lang/Class;)Z", null, null);
    visitor.visitParameter("type", 0);
    visitor.visitCode();
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitLdcInsn(Type.getObjectType(controllerClass.replace(".", "/")));
    Label l0 = new Label();
    visitor.visitJumpInsn(IF_ACMPNE, l0);
    visitor.visitInsn(Opcodes.ICONST_1);
    Label l1 = new Label();
    visitor.visitJumpInsn(Opcodes.GOTO, l1);
    visitor.visitLabel(l0);
    visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    visitor.visitInsn(Opcodes.ICONST_0);
    visitor.visitLabel(l1);
    visitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
    visitor.visitInsn(Opcodes.IRETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }
}
