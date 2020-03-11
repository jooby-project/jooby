/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import io.jooby.Router;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Objects;

public class ContextPathParser {

  public static String parse(ParserContext ctx) {
    ClassNode classNode = ctx.classNode(ctx.getRouter());
    for (MethodNode method : classNode.methods) {
      String contextPath = InsnSupport.next(method.instructions.getFirst())
          .filter(MethodInsnNode.class::isInstance)
          .map(MethodInsnNode.class::cast)
          .filter(i -> {
            Signature signature = Signature.create(i);
            return signature.matches("setContextPath", String.class);
          })
          .findFirst()
          .map(i ->
              InsnSupport.prev(i)
                  .filter(LdcInsnNode.class::isInstance)
                  .findFirst()
                  .map(LdcInsnNode.class::cast)
                  .map(c -> c.cst.toString())
                  .orElse(null)
          )
          .filter(Objects::nonNull)
          .orElse(null);
      if (contextPath != null) {
        return Router.normalizePath(contextPath);
      }
    }
    return "/";
  }
}
