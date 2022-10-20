/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.asm;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ReturnType {

  private static final Predicate<AbstractInsnNode> LABEL = LabelNode.class::isInstance;

  private static final Predicate<AbstractInsnNode> LINE_NUMBER = LineNumberNode.class::isInstance;

  private static final Predicate<AbstractInsnNode> KT_INTERNAL =
      it -> {
        if (it instanceof MethodInsnNode) {
          return ((MethodInsnNode) it).owner.startsWith("kotlin/jvm/internal");
        }
        return false;
      };

  private static final Predicate<AbstractInsnNode> IGNORE = KT_INTERNAL.or(LABEL).or(LINE_NUMBER);

  private static final Predicate<AbstractInsnNode> KT_LDC =
      it -> {
        if (it instanceof LdcInsnNode) {
          return it.getPrevious() != null && it.getPrevious().getOpcode() == Opcodes.DUP;
        }
        return false;
      };

  public static java.lang.reflect.Type find(TypeParser typeParser, MethodNode node) {
    List<AbstractInsnNode> returns = findReturns(node);
    Set<java.lang.reflect.Type> types = new LinkedHashSet<>();
    for (AbstractInsnNode aReturn : returns) {
      AbstractInsnNode previous = previous(aReturn.getPrevious());
      if (previous instanceof MethodInsnNode) {
        MethodInsnNode call = (MethodInsnNode) previous;
        // Constructor?
        if (call.name.equals("<init>")) {
          types.add(typeParser.resolve(call.owner));
        } else {
          types.add(typeParser.parseTypeDescriptor(Type.getReturnType(call.desc).getDescriptor()));
        }
      } else if (previous instanceof VarInsnNode) {
        localVariable(typeParser, node, (VarInsnNode) previous).ifPresent(types::add);
      } else if (previous instanceof InvokeDynamicInsnNode) {
        // visitInvokeDynamicInsn("call", "()Ljava/util/concurrent/Callable;", new
        // Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
        // "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false), new Object[]{Type.getType("()Ljava/lang/Object;"), new Handle(Opcodes.H_INVOKESTATIC, "io/jooby/internal/ReturnTypeTest", "lambda$null$11", "()Ljava/lang/Character;", false), Type.getType("()Ljava/lang/Character;")});
        // (Callable<Character>) () -> 'x'
        InvokeDynamicInsnNode invokeDynamic = (InvokeDynamicInsnNode) previous;
        String handleDescriptor =
            Stream.of(invokeDynamic.bsmArgs)
                .filter(Handle.class::isInstance)
                .map(Handle.class::cast)
                .findFirst()
                .map(
                    h -> {
                      String desc = Type.getReturnType(h.getDesc()).getDescriptor();
                      return "V".equals(desc) ? "java/lang/Object" : desc;
                    })
                .orElse(null);
        // Ljava/util/concurrent/Callable;
        String descriptor = Type.getReturnType(invokeDynamic.desc).getDescriptor();
        if (handleDescriptor != null) {
          // Handle: ()Ljava/lang/Character;
          if (descriptor.endsWith(";")) {
            descriptor = descriptor.substring(0, descriptor.length() - 1);
          }
          descriptor += "<" + handleDescriptor + ">";
        }
        types.add(typeParser.parseTypeDescriptor(descriptor));
      } else if (previous instanceof LdcInsnNode) {
        /** return "String" | int | double */
        Object cst = ((LdcInsnNode) previous).cst;
        if (cst instanceof Type) {
          types.add(typeParser.parseTypeDescriptor(((Type) cst).getDescriptor()));
        } else {
          types.add(cst.getClass());
        }
      } else {
        // array
        switch (previous.getOpcode()) {
          case Opcodes.NEWARRAY:
            {
              ofNullable(primitiveEmptyArray(previous)).ifPresent(types::add);
            }
            break;
          case Opcodes.ANEWARRAY:
            {
              TypeInsnNode typeInsn = (TypeInsnNode) previous;
              types.add(typeParser.parseTypeDescriptor("[" + typeInsn.desc));
            }
            break;
          case Opcodes.BASTORE:
            types.add(boolean[].class);
            break;
          case Opcodes.CASTORE:
            types.add(char[].class);
            break;
          case Opcodes.SASTORE:
            types.add(short[].class);
            break;
          case Opcodes.IASTORE:
            types.add(int[].class);
            break;
          case Opcodes.LASTORE:
            types.add(long[].class);
            break;
          case Opcodes.FASTORE:
            types.add(float[].class);
            break;
          case Opcodes.DASTORE:
            types.add(double[].class);
            break;
          case Opcodes.AASTORE:
            return Insns.previous(previous)
                .filter(e -> e.getOpcode() == Opcodes.ANEWARRAY)
                .findFirst()
                .map(
                    e -> {
                      TypeInsnNode typeInsn = (TypeInsnNode) e;
                      return typeParser.parseTypeDescriptor("[" + typeInsn.desc);
                    })
                .orElse(Object.class);
        }
      }
    }
    return types.isEmpty() ? Object.class : typeParser.commonAncestor(types);
  }

  private static Class primitiveEmptyArray(AbstractInsnNode previous) {
    // empty primitive array
    if (previous instanceof IntInsnNode) {
      switch (((IntInsnNode) previous).operand) {
        case Opcodes.T_BOOLEAN:
          return boolean[].class;
        case Opcodes.T_CHAR:
          return char[].class;
        case Opcodes.T_BYTE:
          return byte[].class;
        case Opcodes.T_SHORT:
          return short[].class;
        case Opcodes.T_INT:
          return int[].class;
        case Opcodes.T_LONG:
          return long[].class;
        case Opcodes.T_FLOAT:
          return float[].class;
        case Opcodes.T_DOUBLE:
          return double[].class;
      }
    }
    return null;
  }

  private static AbstractInsnNode previous(AbstractInsnNode it) {
    while (it != null) {
      if (IGNORE.test(it)) {
        it = it.getPrevious();
      } else {
        if (KT_LDC.test(it)) {
          it = it.getPrevious().getPrevious();
        } else {
          return it;
        }
      }
    }
    return null;
  }

  private static List<AbstractInsnNode> findReturns(MethodNode node) {
    List<AbstractInsnNode> result = new ArrayList<>();
    for (AbstractInsnNode instruction : node.instructions) {
      if (instruction.getOpcode() == Opcodes.ARETURN) {
        result.add(instruction);
      }
    }
    return result;
  }

  private static Optional<java.lang.reflect.Type> localVariable(
      TypeParser typeParser, MethodNode m, VarInsnNode varInsn) {
    if (varInsn.getOpcode() == Opcodes.ALOAD) {
      List<LocalVariableNode> vars = m.localVariables;
      LocalVariableNode var =
          vars.stream().filter(v -> v.index == varInsn.var).findFirst().orElse(null);
      if (var != null) {
        String signature = ofNullable(var.signature).orElse(var.desc);
        return Optional.of(typeParser.parseTypeDescriptor(signature));
      }
    }
    return Optional.empty();
  }
}
