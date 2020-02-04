package io.jooby.internal.openapi;

import io.jooby.HandlerContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RouteReturnTypeParser {

  public static Set<String> parse(ExecutionContext ctx, MethodNode node) {
    Set<String> result = InsnSupport.next(node.instructions.getFirst())
        .filter(it -> it.getOpcode() == Opcodes.ARETURN || it.getOpcode() == Opcodes.IRETURN
            || it.getOpcode() == Opcodes.RETURN)
        .map(it -> {
          if (it.getOpcode() == Opcodes.RETURN) {
            return Object.class.getName();
          }
          /** IRETURN */
          if (it.getOpcode() == Opcodes.IRETURN) {
            if (it instanceof InsnNode) {
              AbstractInsnNode prev = it.getPrevious();
              if (prev instanceof IntInsnNode) {
                return Integer.class.getName();
              }
              if (prev instanceof InsnNode) {
                if (prev.getOpcode() == Opcodes.ICONST_0
                    || prev.getOpcode() == Opcodes.ICONST_1) {
                  return Boolean.class.getName();
                }
              }
            }
          }

          for (Iterator<AbstractInsnNode> iterator = InsnSupport
              .prevIterator(it.getPrevious()); iterator.hasNext(); ) {
            AbstractInsnNode i = iterator.next();
            if (i instanceof MethodInsnNode && (((MethodInsnNode) i).owner
                .equals("kotlin/jvm/internal/Intrinsics"))) {
              // skip Ldc and load var
              // dup or aload
              // visitLdcInsn("$receiver");
              // visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
              iterator.next();
              iterator.next();
              continue;
            }
            if (i instanceof MethodInsnNode && (((MethodInsnNode) i).owner
                .equals("kotlin/TypeCastException"))) {
              continue;
            }
            if (i instanceof LineNumberNode || i instanceof LabelNode) {
              continue;
            }
            String sourcedesc = null;
            /** return 1; return true; return new Foo(); */
            if (i instanceof MethodInsnNode) {
              MethodInsnNode minnsn = (MethodInsnNode) i;
              if (minnsn.name.equals("<init>")) {
                return Type.getObjectType(minnsn.owner).getClassName();
              }
              if (i.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                AbstractInsnNode invokeDynamic = InsnSupport.prev(i)
                    .filter(InvokeDynamicInsnNode.class::isInstance)
                    .findFirst()
                    .orElse(null);
                if (invokeDynamic != null) {
                  sourcedesc = minnsn.desc;
                  i = invokeDynamic;
                } else {
                  return fromMethodCall(ctx, minnsn);
                }
              } else {
                return fromMethodCall(ctx, minnsn);
              }
            }
            /** return "String" | int | double */
            if (i instanceof LdcInsnNode) {
              Object cst = ((LdcInsnNode) i).cst;
              if (cst instanceof Type) {
                return ((Type) cst).getClassName();
              }
              return cst.getClass().getName();
            }

            /** return variable */
            if (i instanceof VarInsnNode) {
              VarInsnNode varInsn = (VarInsnNode) i;
              return localVariable(ctx, node, varInsn);
            }
            /** Invoke dynamic: */
            if (i instanceof InvokeDynamicInsnNode) {
              InvokeDynamicInsnNode invokeDynamic = (InvokeDynamicInsnNode) i;
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
              if (handleDescriptor != null && !handleDescriptor.equals("java/lang/Object")) {
                if (descriptor.endsWith(";")) {
                  descriptor = descriptor.substring(0, descriptor.length() - 1);
                }
                descriptor += "<" + handleDescriptor + ">;";
              }
              return parseSignature(descriptor);
            }
            /** array literal: */
            if (i.getOpcode() == Opcodes.NEWARRAY) {
              // empty primitive array
              if (i instanceof IntInsnNode) {
                switch (((IntInsnNode) i).operand) {
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
            if (i.getOpcode() == Opcodes.ANEWARRAY) {
              TypeInsnNode typeInsn = (TypeInsnNode) i;
              return parseSignature("[L" + typeInsn.desc + ";");
            }
            // non empty array
            switch (i.getOpcode()) {
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
                return InsnSupport.prev(i)
                    .filter(e -> e.getOpcode() == Opcodes.ANEWARRAY)
                    .findFirst()
                    .map(e -> {
                      TypeInsnNode typeInsn = (TypeInsnNode) e;
                      return parseSignature("[L" + typeInsn.desc + ";");
                    })
                    .orElse(Object.class.getName());
            }
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
            .map(RouteReturnTypeParser::parseSignature)
            .orElseGet(() -> Type.getReturnType(m.desc).getClassName())
        )
        .orElse(returnType.getClassName());
  }

  private static String localVariable(final ExecutionContext ctx, final MethodNode m,
      final VarInsnNode varInsn) {
    int opcode = varInsn.getOpcode();
    if (opcode >= Opcodes.ILOAD && opcode <= Opcodes.ISTORE) {
      List<LocalVariableNode> vars = m.localVariables;
      LocalVariableNode var = vars.stream()
          .filter(v -> v.index == varInsn.var)
          .findFirst()
          .orElse(null);
      if (var != null) {
        if (var.signature == null) {
          Optional<AbstractInsnNode> kt = InsnSupport.prev(varInsn).filter(kotlinIntrinsics())
              .findFirst();
          if (kt.isPresent()) {
            LocalVariableNode $this = vars.stream().filter(v -> v.name.equals("this"))
                .findFirst()
                .orElse(null);
            if ($this != null) {
              Type kotlinLambda = Type.getType($this.desc);
              ClassNode classNode = ctx.classNodeOrNull(kotlinLambda);
              if (classNode != null && classNode.signature != null) {
                String type = parseSignature(classNode.signature, internalName ->
                    !internalName.equals("kotlin/jvm/internal/Lambda")
                        && !internalName.equals("kotlin/jvm/functions/Function1")
                        && !internalName.equals("io/jooby/HandlerContext")
                );
                return type;
              }
            }
          }
          return parseSignature(var.desc);
        }
        return parseSignature(var.signature);
      }
    }
    return Object.class.getName();
  }

  private static Predicate<AbstractInsnNode> kotlinIntrinsics() {
    return i -> (i instanceof MethodInsnNode && ((MethodInsnNode) i).owner
        .equals("kotlin/jvm/internal/Intrinsics"));
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

  private static String parseSignature(String signature) {
    return parseSignature(signature, type -> true);
  }

  private static String parseSignature(String signature, Predicate<String> filter) {
    SignatureReader reader = new SignatureReader(signature);
    LinkedList<TypeName> stack = new LinkedList<>();
    SignatureVisitor visitor = new SignatureVisitor(Opcodes.ASM7) {
      @Override public void visitClassType(String name) {
        if (filter.test(name)) {
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
        return this;
      }
    };
    reader.accept(visitor);
    TypeName type = stack.pop();
    return type.toString();
  }

  public static void foo(Function<HandlerContext, CompletableFuture<Integer>> fn) {
  }

  public static void main(String[] args) throws NoSuchMethodException {
    Method foo = RouteReturnTypeParser.class.getDeclaredMethod("foo", Function.class);
    System.out.println(Type.getMethodDescriptor(foo));
  }
}
