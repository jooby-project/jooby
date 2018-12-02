package io.jooby.internal.asm;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jooby.internal.asm.Insns.last;

public class ReturnType extends MethodVisitor {

  private static final Predicate<AbstractInsnNode> LABEL = LabelNode.class::isInstance;

  private static final Predicate<AbstractInsnNode> LINE_NUMBER = LineNumberNode.class::isInstance;

  private final TypeParser typeParser;

  public ReturnType(TypeParser typeParser, MethodNode visitor) {
    super(Opcodes.ASM6, visitor);
    this.typeParser = typeParser;
  }

  public java.lang.reflect.Type returnType() {
    MethodNode node = (MethodNode) mv;
    Set<java.lang.reflect.Type> result = last(node.instructions)
        .filter(it -> it.getOpcode() == Opcodes.ARETURN)
        .map(it -> {
          AbstractInsnNode previous = Insns.previous(it.getPrevious())
              .filter(LABEL.negate().and(LINE_NUMBER.negate()))
              .findFirst()
              .orElse(it.getPrevious());
          String sourcedesc = null;
          /** return 1; return true; return new Foo(); */
          if (previous instanceof MethodInsnNode) {
            MethodInsnNode minnsn = (MethodInsnNode) previous;
            if (minnsn.name.equals("<init>")) {
              return typeParser.resolve(minnsn.owner);
            }
            if (previous.getOpcode() == Opcodes.INVOKEVIRTUAL) {
              AbstractInsnNode invokeDynamic = Insns.previous(previous)
                  .filter(InvokeDynamicInsnNode.class::isInstance)
                  .findFirst()
                  .orElse(null);
              if (invokeDynamic != null) {
                sourcedesc = minnsn.desc;
                previous = invokeDynamic;
              } else {
                return typeParser
                    .parseTypeDescriptor(Type.getReturnType(minnsn.desc).getDescriptor());
              }
            } else if (previous.getOpcode() == Opcodes.INVOKESPECIAL) {
              try {
                MethodInsnNode invokeSpecial = (MethodInsnNode) previous;
                Class owner = typeParser.resolve(invokeSpecial.owner);
                Class[] args = Stream.of(Type.getArgumentTypes(invokeSpecial.desc))
                    .map(e -> typeParser.resolve(e.getClassName()))
                    .toArray(Class[]::new);
                Method reference = owner.getDeclaredMethod(invokeSpecial.name, args);
                return reference.getGenericReturnType();
              } catch (NoSuchMethodException x) {
                return Object.class;
              }
            } else {
              return typeParser
                  .parseTypeDescriptor(Type.getReturnType(minnsn.desc).getDescriptor());
            }
          }
          /** return "String" | int | double */
          if (previous instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) previous).cst;
            if (cst instanceof Type) {
              return typeParser.parseTypeDescriptor(((Type) cst).getDescriptor());
            }
            return cst.getClass();
          }
          /** return variable */
          if (previous instanceof VarInsnNode) {
            VarInsnNode varInsn = (VarInsnNode) previous;
            return localVariable(node, varInsn);
          }
          /** Invoke dynamic: */
          if (previous instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode invokeDynamic = (InvokeDynamicInsnNode) previous;
            String handleDescriptor = Stream.of(invokeDynamic.bsmArgs)
                .filter(Handle.class::isInstance)
                .map(Handle.class::cast)
                .findFirst()
                .map(h -> {
                  String desc = Type.getReturnType(h.getDesc()).getDescriptor();
                  return "V".equals(desc) ? "java/lang/Object" : desc;
                })
                .orElse(null);
            String descriptor = Type
                .getReturnType(Optional.ofNullable(sourcedesc).orElse(invokeDynamic.desc))
                .getDescriptor();
            if (handleDescriptor != null) {
              if (descriptor.endsWith(";")) {
                descriptor = descriptor.substring(0, descriptor.length() - 1);
              }
              descriptor += "<" + handleDescriptor + ">";
            }
            return typeParser.parseTypeDescriptor(descriptor);
          }
          /** array literal: */
          if (previous.getOpcode() == Opcodes.NEWARRAY) {
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
          }
          // empty array of objects
          if (previous.getOpcode() == Opcodes.ANEWARRAY) {
            TypeInsnNode typeInsn = (TypeInsnNode) previous;
            return typeParser.parseTypeDescriptor("[" + typeInsn.desc);
          }
          // non empty array
          switch (previous.getOpcode()) {
            case Opcodes.BASTORE:
              return boolean[].class;
            case Opcodes.CASTORE:
              return char[].class;
            case Opcodes.SASTORE:
              return short[].class;
            case Opcodes.IASTORE:
              return int[].class;
            case Opcodes.LASTORE:
              return long[].class;
            case Opcodes.FASTORE:
              return float[].class;
            case Opcodes.DASTORE:
              return double[].class;
            case Opcodes.AASTORE:
              return Insns.previous(previous).filter(e -> e.getOpcode() == Opcodes.ANEWARRAY)
                  .findFirst()
                  .map(e -> {
                    TypeInsnNode typeInsn = (TypeInsnNode) e;
                    return typeParser.parseTypeDescriptor("[" + typeInsn.desc);
                  })
                  .orElse(Object.class);
          }

          return Object.class;
        })
        .collect(Collectors.toSet());
    return typeParser.commonAncestor(result);
  }

  private java.lang.reflect.Type localVariable(final MethodNode m, final VarInsnNode varInsn) {
    if (varInsn.getOpcode() == Opcodes.ALOAD) {
      List<LocalVariableNode> vars = m.localVariables;
      LocalVariableNode var = vars.stream()
          .filter(v -> v.index == varInsn.var)
          .findFirst()
          .orElse(null);
      if (var != null) {
        String signature = Optional.ofNullable(var.signature).orElse(var.desc);
        return typeParser.parseTypeDescriptor(signature);
      }
    }
    return Object.class;
  }
}
