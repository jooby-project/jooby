package io.jooby.internal.openapi;

import io.jooby.internal.asm.Insns;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RouteReturnTypeParser {

  public static Set<String> parse(ExecutionContext ctx, MethodNode node) {
    Set<String> result = InsnSupport.next(node.instructions.getFirst())
        .filter(it -> it.getOpcode() == Opcodes.ARETURN)
        .map(it -> {
          AbstractInsnNode previous = InsnSupport.prev(it.getPrevious())
              .filter(e -> (!(e instanceof LabelNode) && !(e instanceof LineNumberNode)))
              .findFirst()
              .orElse(it.getPrevious());
          String sourcedesc = null;
          /** return 1; return true; return new Foo(); */
          if (previous instanceof MethodInsnNode) {
            MethodInsnNode minnsn = (MethodInsnNode) previous;
            if (minnsn.name.equals("<init>")) {
              return Type.getObjectType(minnsn.owner).getClassName();
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
                return fromMethodCall(ctx, minnsn);
              }
            } else {
              return fromMethodCall(ctx, minnsn);
            }
          }
          /** return "String" | int | double */
          if (previous instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) previous).cst;
            if (cst instanceof Type) {
              return ((Type) cst).getClassName();
            }
            return cst.getClass().getName();
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
                  String desc = org.objectweb.asm.Type.getReturnType(h.getDesc()).getDescriptor();
                  return "V".equals(desc) ? "java/lang/Object" : desc;
                })
                .orElse(null);
            String descriptor = org.objectweb.asm.Type
                .getReturnType(Optional.ofNullable(sourcedesc).orElse(invokeDynamic.desc))
                .getDescriptor();
            if (handleDescriptor != null && !handleDescriptor.equals("java/lang/Object")) {
              if (descriptor.endsWith(";")) {
                descriptor = descriptor.substring(0, descriptor.length() - 1);
              }
              descriptor += "<" + handleDescriptor + ">;";
            }
            return signatureToJavaType(descriptor);
          }
          /** array literal: */
          if (previous.getOpcode() == Opcodes.NEWARRAY) {
            // empty primitive array
            if (previous instanceof IntInsnNode) {
              switch (((IntInsnNode) previous).operand) {
                case Opcodes.T_BOOLEAN:
                  return boolean[].class.getName();
                case Opcodes.T_CHAR:
                  return char[].class.getName();
                case Opcodes.T_BYTE:
                  return byte[].class.getName();
                case Opcodes.T_SHORT:
                  return short[].class.getName();
                case Opcodes.T_INT:
                  return int[].class.getName();
                case Opcodes.T_LONG:
                  return long[].class.getName();
                case Opcodes.T_FLOAT:
                  return float[].class.getName();
                case Opcodes.T_DOUBLE:
                  return double[].class.getName();
              }
            }
          }
          // empty array of objects
          if (previous.getOpcode() == Opcodes.ANEWARRAY) {
            TypeInsnNode typeInsn = (TypeInsnNode) previous;
            return signatureToJavaType("[L" + typeInsn.desc + ";");
          }
          // non empty array
          switch (previous.getOpcode()) {
            case Opcodes.BASTORE:
              return boolean[].class.getName();
            case Opcodes.CASTORE:
              return char[].class.getName();
            case Opcodes.SASTORE:
              return short[].class.getName();
            case Opcodes.IASTORE:
              return int[].class.getName();
            case Opcodes.LASTORE:
              return long[].class.getName();
            case Opcodes.FASTORE:
              return float[].class.getName();
            case Opcodes.DASTORE:
              return double[].class.getName();
            case Opcodes.AASTORE:
              return Insns.previous(previous)
                  .filter(e -> e.getOpcode() == Opcodes.ANEWARRAY)
                  .findFirst()
                  .map(e -> {
                    TypeInsnNode typeInsn = (TypeInsnNode) e;
                    return signatureToJavaType("[L" + typeInsn.desc + ";");
                  })
                  .orElse(Object.class.getName());
          }

          return Object.class.getName();
        })
        .map(Object::toString)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return result;
  }

  private static String fromMethodCall(ExecutionContext ctx, MethodInsnNode node) {
    Type returnType = Type.getReturnType(node.desc);
    ClassNode classNode;
    try {
      classNode = ctx.classNode(Type.getObjectType(node.owner));
    } catch (Exception x) {
      return returnType.getClassName();
    }
    return classNode.methods.stream()
        .filter(m -> m.name.equals(node.name) && m.desc.equals(node.desc))
        .findFirst()
        .map(m -> Optional.ofNullable(m.signature)
            .map(RouteReturnTypeParser::signatureToJavaType)
            .orElseGet(() -> Type.getReturnType(m.desc).getClassName())
        )
        .orElse(returnType.getClassName());
  }

  private static String localVariable(final MethodNode m, final VarInsnNode varInsn) {
    if (varInsn.getOpcode() == Opcodes.ALOAD) {
      List<LocalVariableNode> vars = m.localVariables;
      LocalVariableNode var = vars.stream()
          .filter(v -> v.index == varInsn.var)
          .findFirst()
          .orElse(null);
      if (var != null) {
        return signatureToJavaType(var.signature == null ? var.desc : var.signature);
      }
    }
    return Object.class.getName();
  }

  private static class TypeName {
    String name;
    String prefix;
    String suffix;
    List<TypeName> arguments = new ArrayList<>();

    @Override public String toString() {
      StringBuilder buff = new StringBuilder();
      if (prefix != null) {
        buff.append(prefix);
      }
      buff.append(name.replace("/", "."));
      if (arguments.size() > 0) {
        String argstring = arguments.stream().map(TypeName::toString)
            .collect(Collectors.joining(",", "<", ">"));
        buff.append(argstring);
      }
      if (suffix != null) {
        buff.append(suffix);
      }
      return buff.toString();
    }
  }

  private static String signatureToJavaType(String signature) {
    SignatureReader reader = new SignatureReader(signature);
    LinkedList<TypeName> stack = new LinkedList<>();
    SignatureVisitor visitor = new SignatureVisitor(Opcodes.ASM7) {
      @Override public void visitClassType(String name) {
        if (stack.isEmpty()) {
          TypeName type = new TypeName();
          type.name = name;
          stack.push(type);
        } else {
          TypeName type = stack.peek();
          if (type.name == null) {
            type.name = name;
          } else {
            TypeName arg = new TypeName();
            arg.name = name;
            type.arguments.add(arg);
            stack.push(arg);
          }
        }
      }

      @Override public void visitEnd() {
        if (stack.size() > 1) {
          stack.pop();
        }
      }

      @Override public void visitBaseType(char descriptor) {
        visitClassType(String.valueOf(descriptor));
        TypeName type = stack.peek();
        type.prefix = "[";
        type.suffix = null;
      }

      @Override public SignatureVisitor visitArrayType() {
        TypeName type = new TypeName();
        type.prefix = "[L";
        type.suffix = ";";
        stack.push(type);
        return super.visitArrayType();
      }
    };
    reader.accept(visitor);
    TypeName type = stack.pop();
    return type.toString();
  }
}
