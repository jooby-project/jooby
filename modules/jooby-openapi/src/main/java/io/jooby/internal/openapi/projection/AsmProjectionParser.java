/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.projection;

import java.util.Optional;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import io.jooby.internal.openapi.InsnSupport;
import io.jooby.internal.openapi.ParserContext;

/**
 * Parses bytecode to find Projected.wrap(...).include("...") definitions. Extracts the target class
 * (with generics when available) and the view string.
 */
public class AsmProjectionParser {

  public static class ProjectionDef {
    public final String targetClass;
    public final String viewString;

    public ProjectionDef(String targetClass, String viewString) {
      this.targetClass = targetClass;
      this.viewString = viewString;
    }
  }

  /**
   * Scans a method's instructions to find a Projection definition.
   *
   * @param methodNode The MethodNode containing the local variable table (LVTT).
   * @return The parsed ProjectionDef, or empty if not found.
   */
  public static Optional<ProjectionDef> parse(ParserContext ctx, MethodNode methodNode) {
    if (methodNode == null
        || methodNode.instructions == null
        || methodNode.instructions.size() == 0) {
      return Optional.empty();
    }

    // Start from the first instruction in the method
    AbstractInsnNode methodStart = methodNode.instructions.getFirst();

    // 1. Find `Projected.include(String)`
    MethodInsnNode includeNode =
        (MethodInsnNode)
            InsnSupport.next(methodStart)
                .filter(n -> n.getOpcode() == Opcodes.INVOKEVIRTUAL)
                .filter(n -> n instanceof MethodInsnNode)
                .filter(
                    n -> {
                      MethodInsnNode mn = (MethodInsnNode) n;
                      return "io/jooby/Projected".equals(mn.owner) && "include".equals(mn.name);
                    })
                .findFirst()
                .orElse(null);

    if (includeNode == null) {
      return Optional.empty();
    }

    // 2. Extract View String (e.g., "(id, name)")
    String viewString =
        InsnSupport.prev(includeNode)
            .filter(n -> n.getOpcode() == Opcodes.LDC && ((LdcInsnNode) n).cst instanceof String)
            .map(n -> (String) ((LdcInsnNode) n).cst)
            .findFirst()
            .orElse(null);

    if (viewString == null) {
      return Optional.empty();
    }

    // 3. Find `wrap(...)` and trace back to its payload
    MethodInsnNode wrapNode = findWrapNode(includeNode);
    String targetClass = null;

    if (wrapNode != null) {
      AbstractInsnNode payloadNode = wrapNode.getPrevious();

      // If it's a variable: `Projected.wrap(myList)`
      if (payloadNode.getOpcode() == Opcodes.ALOAD) {
        int varIndex = ((VarInsnNode) payloadNode).var;

        // Try to get the generic signature from the Local Variable Type Table (LVTT)
        targetClass = extractFromLocalVariables(methodNode, varIndex);

        if (targetClass == null) {
          // Fallback: trace to ASTORE if LVTT is missing or doesn't have generics
          payloadNode = findAstore(wrapNode, varIndex);
        }
      }

      // If it wasn't a variable, or LVTT failed, fall back to instruction heuristics
      if (targetClass == null && payloadNode != null) {
        targetClass = extractTypeWithGenerics(ctx, payloadNode);
      }
    }

    return Optional.of(new ProjectionDef(targetClass, viewString));
  }

  // --- Data-Flow Helpers ---

  private static MethodInsnNode findWrapNode(AbstractInsnNode includeNode) {
    AbstractInsnNode prev = includeNode.getPrevious();
    while (prev != null) {
      if (isWrapCall(prev)) {
        return (MethodInsnNode) prev;
      }
      if (prev.getOpcode() == Opcodes.ALOAD) {
        int varIndex = ((VarInsnNode) prev).var;
        AbstractInsnNode assignment = findAstore(prev, varIndex);
        if (assignment != null && isWrapCall(assignment.getPrevious())) {
          return (MethodInsnNode) assignment.getPrevious();
        }
      }
      prev = prev.getPrevious();
    }
    return null;
  }

  private static boolean isWrapCall(AbstractInsnNode n) {
    if (n != null && n.getOpcode() == Opcodes.INVOKESTATIC && n instanceof MethodInsnNode) {
      MethodInsnNode mn = (MethodInsnNode) n;
      return "io/jooby/Projected".equals(mn.owner) && "wrap".equals(mn.name);
    }
    return false;
  }

  private static AbstractInsnNode findAstore(AbstractInsnNode start, int varIndex) {
    AbstractInsnNode prev = start.getPrevious();
    while (prev != null) {
      if (prev.getOpcode() == Opcodes.ASTORE && ((VarInsnNode) prev).var == varIndex) {
        return prev.getPrevious();
      }
      prev = prev.getPrevious();
    }
    return null;
  }

  // --- Local Variable Type Table (LVTT) Resolution ---

  private static String extractFromLocalVariables(MethodNode methodNode, int varIndex) {
    if (methodNode.localVariables == null) {
      return null;
    }

    for (LocalVariableNode lvn : methodNode.localVariables) {
      if (lvn.index == varIndex) {
        // Prefer the full generic signature if available
        if (lvn.signature != null) {
          return parseAsmSignature(lvn.signature);
        }
        // Fallback to the erased descriptor (e.g., Ljava/util/List;)
        if (lvn.desc != null) {
          return Type.getType(lvn.desc).getClassName();
        }
      }
    }
    return null;
  }

  /**
   * Converts an ASM generic signature to a clean Java class name. Input:
   * Ljava/util/List<Ltests/i3853/U3853;>; Output: java.util.List<tests.i3853.U3853>
   */
  private static String parseAsmSignature(String signature) {
    if (signature == null) return null;

    // 1. Detect and erase unresolved Type Variables (like TE; or TId;)
    // In JVM signatures, type variables always start with 'T', end with ';',
    // and are immediately preceded by '<', ';', '[', or ')'
    signature = signature.replaceAll("(?<=[<;\\[)])T[^;]+;", "Ljava/lang/Object;");

    // 2. Convert slashes to dots
    String cleaned = signature.replace('/', '.');

    // 3. Strip the outermost 'L' and ';'
    if (cleaned.startsWith("L")) {
      cleaned = cleaned.substring(1);
    }
    if (cleaned.endsWith(";")) {
      cleaned = cleaned.substring(0, cleaned.length() - 1);
    }

    // 4. Handle multiple generic arguments (separated by ;L)
    cleaned = cleaned.replace(";L", ", ");

    // 5. Strip the 'L' that appears right after the opening bracket
    cleaned = cleaned.replace("<L", "<");

    // 6. Remove any remaining semicolons
    cleaned = cleaned.replace(";", "");

    return cleaned;
  }

  // --- Instruction Heuristics (For inline method calls & instantiations) ---

  private static String extractTypeWithGenerics(ParserContext ctx, AbstractInsnNode node) {
    if (node.getOpcode() == Opcodes.LDC && ((LdcInsnNode) node).cst instanceof Type) {
      return ((Type) ((LdcInsnNode) node).cst).getClassName();
    }

    if (node.getOpcode() == Opcodes.NEW) {
      return Type.getObjectType(((TypeInsnNode) node).desc).getClassName();
    }

    if (node instanceof MethodInsnNode) {
      MethodInsnNode mn = (MethodInsnNode) node;
      Type returnType = Type.getReturnType(mn.desc);
      String className = returnType.getClassName();

      // --- 1. FIRST PRIORITY: Inline Container Heuristics ---
      // If it's List.of(), Set.of(), we can infer the exact type from the arguments
      // passed into it, which is vastly superior to the method's raw <TE> signature.
      if (isContainerClass(className) && mn.getOpcode() == Opcodes.INVOKESTATIC) {
        String genericParam = inferGenericArgument(mn);
        if (genericParam != null) {
          return className + "<" + genericParam + ">";
        }
      }

      // --- 2. SECOND PRIORITY: Deep Method Signature Resolution ---
      // If it's an instance method like service.list(), try to look up its generic
      // return type using ParserContext.
      if (ctx != null) {
        try {
          Type ownerType = Type.getObjectType(mn.owner);
          MethodNode targetMethod = ctx.findMethodNode(ownerType, mn.name, mn.desc);

          if (targetMethod != null && targetMethod.signature != null) {
            // Find the return type portion of the signature
            int returnTypeStart = targetMethod.signature.lastIndexOf(')');
            if (returnTypeStart != -1) {
              String returnSignature = targetMethod.signature.substring(returnTypeStart + 1);
              String parsed = parseAsmSignature(returnSignature);

              // Only return this if it actually gave us something better than Object.
              // If it just gave us List<Object> because of a <TE> erasure, we might
              // as well fall through to the base class name anyway.
              if (parsed != null && !parsed.contains("<java.lang.Object>")) {
                return parsed;
              }
            }
          }
        } catch (Exception e) {
          // Ignore
        }
      }

      // --- 3. FALLBACK: Return the erased class name ---
      if (!"java.lang.Object".equals(className)) {
        return className;
      }
    }
    return null;
  }

  private static boolean isContainerClass(String className) {
    return "java.util.List".equals(className)
        || "java.util.Set".equals(className)
        || "java.util.Collection".equals(className)
        || "java.util.Optional".equals(className);
  }

  /**
   * Scans backwards to find the type of the argument passed to a container method. Useful for
   * inline calls like: List.of(U3853.createUser())
   */
  private static String inferGenericArgument(MethodInsnNode methodCall) {
    Type[] argTypes = Type.getArgumentTypes(methodCall.desc);

    if (argTypes.length == 0) {
      return null;
    }

    AbstractInsnNode prev = methodCall.getPrevious();

    while (prev != null) {
      if (prev.getOpcode() == Opcodes.NEW) {
        return Type.getObjectType(((TypeInsnNode) prev).desc).getClassName();
      }
      if (prev instanceof MethodInsnNode) {
        MethodInsnNode argMethod = (MethodInsnNode) prev;
        String returnClass = Type.getReturnType(argMethod.desc).getClassName();
        if (!"java.lang.Object".equals(returnClass)) {
          return returnClass;
        }
      }
      prev = prev.getPrevious();
    }

    return null;
  }
}
