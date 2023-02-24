/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.nio.file.Path;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.jooby.internal.apt.MethodDescriptor;
import io.jooby.internal.apt.ParamDefinition;

public class FileUploadWriter implements ParamWriter {
  @Override
  public void accept(
      ClassWriter writer,
      Type controller,
      String handlerInternalName,
      MethodVisitor visitor,
      ParamDefinition parameter,
      NameGenerator nameGenerator)
      throws Exception {
    if (parameter.isList()) {
      files(parameter.getName(), writer);
      visitor.visitMethodInsn(
          INVOKESTATIC,
          handlerInternalName,
          parameter.getName(),
          "(Lio/jooby/Context;)Ljava/util/List;",
          false);
    } else {
      visitor.visitLdcInsn(parameter.getName());

      visitor.visitMethodInsn(
          INVOKEINTERFACE,
          MethodDescriptor.Context.file().getDeclaringType().getInternalName(),
          MethodDescriptor.Context.file().getName(),
          MethodDescriptor.Context.file().getDescriptor(),
          true);

      if (parameter.is(Path.class)) {
        visitor.visitMethodInsn(
            INVOKEINTERFACE,
            MethodDescriptor.FileUpload.path().getDeclaringType().getInternalName(),
            MethodDescriptor.FileUpload.path().getName(),
            MethodDescriptor.FileUpload.path().getDescriptor(),
            true);
      }
    }
  }

  /**
   * Generate a files method like:
   *
   * <pre>{@code
   * List<FileUpload> [paramName](Context ctx) {
   *    List<FileUpload> files = ctx.files("[paramName]");
   *    return files.isEmpty() ? ctx.files() : files;
   * }
   *
   * }</pre>
   *
   * @param parameter
   * @param writer
   * @throws Exception
   */
  private void files(String parameter, ClassWriter writer) throws Exception {
    org.objectweb.asm.MethodVisitor visitor =
        writer.visitMethod(
            ACC_PRIVATE | ACC_FINAL | ACC_STATIC,
            parameter,
            "(Lio/jooby/Context;)Ljava/util/List;",
            "(Lio/jooby/Context;)Ljava/util/List<Lio/jooby/FileUpload;>;",
            null);
    visitor.visitParameter("ctx", 0);
    visitor.visitCode();
    visitor.visitVarInsn(ALOAD, 0);
    visitor.visitLdcInsn(parameter);
    visitor.visitMethodInsn(
        INVOKEINTERFACE,
        MethodDescriptor.Context.filesWithName().getDeclaringType().getInternalName(),
        MethodDescriptor.Context.filesWithName().getName(),
        MethodDescriptor.Context.filesWithName().getDescriptor(),
        true);
    visitor.visitVarInsn(ASTORE, 1);
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true);
    Label label0 = new Label();
    visitor.visitJumpInsn(IFEQ, label0);
    visitor.visitVarInsn(ALOAD, 0);
    visitor.visitMethodInsn(
        INVOKEINTERFACE,
        MethodDescriptor.Context.files().getDeclaringType().getInternalName(),
        MethodDescriptor.Context.files().getName(),
        MethodDescriptor.Context.files().getDescriptor(),
        true);
    Label label1 = new Label();
    visitor.visitJumpInsn(GOTO, label1);
    visitor.visitLabel(label0);
    visitor.visitFrame(Opcodes.F_APPEND, 1, new Object[] {"java/util/List"}, 0, null);
    visitor.visitVarInsn(ALOAD, 1);
    visitor.visitLabel(label1);
    visitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/util/List"});
    visitor.visitInsn(ARETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }
}
