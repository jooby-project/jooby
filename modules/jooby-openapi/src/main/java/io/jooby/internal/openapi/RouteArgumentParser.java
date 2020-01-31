package io.jooby.internal.openapi;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RouteArgumentParser {

  public static List<RouteArgument> parse(ExecutionContext ctx, MethodNode node) {
    List<MethodInsnNode> methodInsnNodes = StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(node.instructions.iterator(), Spliterator.ORDERED),
        false)
        .filter(MethodInsnNode.class::isInstance)
        .map(MethodInsnNode.class::cast)
        .filter(i -> i.owner.equals("io/jooby/Context"))
        .collect(Collectors.toList());
    List<RouteArgument> args = new ArrayList<>();
    for (MethodInsnNode methodInsnNode : methodInsnNodes) {
      Signature signature = Signature.create(methodInsnNode);
      RouteArgument argument = new RouteArgument();
      if (signature.matches("path")) {
        argument.setHttpType(HttpType.PATH);
        if (signature.matches(String.class)) {
          argument.setName(argumentName(methodInsnNode));
          argumentType(argument, methodInsnNode);
        } else if (signature.matches(Class.class)) {
          argument.setName(signature.getMethod());
          argumentContextToType(argument, methodInsnNode);
        } else {
          // Unsupported query usage
        }
      } else if (signature.matches("query")) {
        argument.setHttpType(HttpType.QUERY);
        if (signature.matches(String.class)) {
          argument.setName(argumentName(methodInsnNode));
          argumentType(argument, methodInsnNode);
        } else if (signature.matches(Class.class)) {
          argument.setName(signature.getMethod());
          argumentContextToType(argument, methodInsnNode);
        } else {
          // Unsupported query usage
        }
      } else if (signature.matches("form") || signature.matches("multipart")) {
        argument.setHttpType(HttpType.FORM);
        if (signature.matches(String.class)) {
          argument.setName(argumentName(methodInsnNode));
          argumentType(argument, methodInsnNode);
        } else if (signature.matches(Class.class)) {
          argument.setName(signature.getMethod());
          argumentContextToType(argument, methodInsnNode);
        } else {
          // Unsupported query usage
        }
      }

      if (argument.getJavaType() != null) {
        args.add(argument);
      } else {
        // Unsupported query usage
      }
    }
    return args;
  }

   private static void argumentContextToType(RouteArgument argument, MethodInsnNode node) {
    Type type = InsnSupport.prev(node)
        .filter(LdcInsnNode.class::isInstance)
        .findFirst()
        .map(LdcInsnNode.class::cast)
        .filter(i -> i.cst instanceof Type)
        .map(i -> (Type) i.cst)
        .orElseThrow(() -> new IllegalStateException(
            "Parameter type not found, for: " + argument.getName()));
    argument.setJavaType(type.getClassName());
    argument.setSingle(false);
  }

  private static void argumentType(RouteArgument argument, MethodInsnNode node) {
    MethodInsnNode convertCall = InsnSupport.next(node)
        .filter(valueOwner())
        .map(MethodInsnNode.class::cast)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Parameter type not found, for: " + argument.getName()));
    Signature convert = Signature.create(convertCall);
    if (convert.matches("value") || convert.matches("valueOrNull") || convert.getMethod()
        .endsWith("Value")) {
      argument.setJavaType(Type.getReturnType(convertCall.desc).getClassName());
      if (convert.matches("valueOrNull")) {
        argument.setRequired(false);
      } else {
        if (convert.getParameterCount() == 0) {
          argument.setRequired(true);
        } else {
          argument.setRequired(false);
          argument.setDefaultValue(argumentDefaultValue(convertCall.getPrevious()));
        }
      }
    } else if (convert.matches("toList")) {
      argument.setJavaType(toGenericOne(convertCall, convert, List.class));
      argument.setRequired(false);
    } else if (convert.matches("toSet")) {
      argument.setJavaType(toGenericOne(convertCall, convert, Set.class));
      argument.setRequired(false);
    } else if (convert.matches("toOptional")) {
      argument.setJavaType(toGenericOne(convertCall, convert, Optional.class));
      argument.setRequired(false);
      InsnSupport.next(convertCall)
          .filter(optionalOrElse())
          .findFirst()
          .map(MethodInsnNode.class::cast)
          .ifPresent(elseCall -> {
            // validate the else branch belong to the same toOptional
            InsnSupport.prev(elseCall).filter(valueToOptional())
                .findFirst()
                .map(MethodInsnNode.class::cast)
                .ifPresent(toOptional -> {
                  if (toOptional.equals(convertCall)) {
                    argument.setDefaultValue(argumentDefaultValue(elseCall.getPrevious()));
                  }
                });
          });
    } else if (convert.matches("to")) {
      Type toType = InsnSupport.prev(convertCall)
          .filter(LdcInsnNode.class::isInstance)
          .findFirst()
          .map(LdcInsnNode.class::cast)
          .map(e -> (Type) e.cst)
          .orElseThrow(() -> new IllegalStateException(
              "Parameter type not found: " + InsnSupport.toString(convertCall)));
      argument.setJavaType(toType.getClassName());
    } else if (convert.matches("toEnum")) {
      Type toType = InsnSupport.prev(convertCall)
          .filter(InvokeDynamicInsnNode.class::isInstance)
          .map(InvokeDynamicInsnNode.class::cast)
          .filter(i -> i.name.equals("tryApply"))
          .findFirst()
          .map(i -> (Handle) i.bsmArgs[1])
          .map(h -> Type.getObjectType(h.getOwner()))
          .orElseThrow(() -> new IllegalStateException(
              "Parameter type not found: " + InsnSupport.toString(convertCall)));
      argument.setJavaType(toType.getClassName());
      argument.setRequired(true);
    } else if (convert.matches("toMap")) {
      argument.setJavaType("java.util.Map<java.lang.String,java.lang.String>");
      argument.setRequired(true);
      argument.setSingle(false);
    } else if (convert.matches("toMultimap")) {
      argument.setJavaType("java.util.Map<java.lang.String,java.util.List<java.lang.String>>");
      argument.setRequired(true);
      argument.setSingle(false);
    }
  }

  private static Predicate<AbstractInsnNode> valueOwner() {
    return e -> {
      if (e instanceof MethodInsnNode) {
        return ((MethodInsnNode) e).owner.equals("io/jooby/Value") || ((MethodInsnNode) e).owner
            .equals("io/jooby/ValueNode");
      }
      return false;
    };
  }

  private static Predicate<AbstractInsnNode> optionalOrElse() {
    return e -> (e instanceof MethodInsnNode && (((MethodInsnNode) e).owner
        .equals("java/util/Optional")) && ((MethodInsnNode) e).name.equals("orElse"));
  }

  private static Predicate<AbstractInsnNode> valueToOptional() {
    return valueOwner().and(e -> ((MethodInsnNode) e).name.equals("toOptional"));
  }

  private static String toGenericOne(MethodInsnNode node, Signature signature, Class collectionType) {
    StringBuilder type = new StringBuilder(collectionType.getName());
    type.append("<");
    if (signature.matches(Class.class)) {
      String itemType = InsnSupport.prev(node).filter(LdcInsnNode.class::isInstance)
          .findFirst()
          .map(e -> ((Type) ((LdcInsnNode) e).cst).getClassName())
          .orElse(String.class.getName());
      type.append(itemType);
    } else {
      type.append(String.class.getName());
    }
    type.append(">");
    return type.toString();
  }

  private static Object argumentDefaultValue(AbstractInsnNode n) {
    if (n instanceof LdcInsnNode) {
      Object cst = ((LdcInsnNode) n).cst;
      if (cst instanceof Type) {
        return ((Type) cst).getClassName();
      }
      return cst;
    } else if (n instanceof InsnNode) {
      InsnNode insn = (InsnNode) n;
      switch (insn.getOpcode()) {
        case Opcodes.ICONST_0:
          return 0;
        case Opcodes.ICONST_1:
          return 1;
        case Opcodes.ICONST_2:
          return 2;
        case Opcodes.ICONST_3:
          return 3;
        case Opcodes.ICONST_4:
          return 4;
        case Opcodes.ICONST_5:
          return 5;
        case Opcodes.LCONST_0:
          return 0L;
        case Opcodes.LCONST_1:
          return 1L;
        case Opcodes.FCONST_0:
          return 0f;
        case Opcodes.FCONST_1:
          return 1f;
        case Opcodes.FCONST_2:
          return 2f;
        case Opcodes.DCONST_0:
          return 0d;
        case Opcodes.DCONST_1:
          return 1d;
        case Opcodes.ICONST_M1:
          return -1;
        case Opcodes.ACONST_NULL:
          return null;
      }
    } else if (n instanceof IntInsnNode) {
      return ((IntInsnNode) n).operand;
    }
    return null;
  }

  private static String argumentName(MethodInsnNode node) {
    return InsnSupport.prev(node)
        .filter(LdcInsnNode.class::isInstance)
        .map(it -> ((LdcInsnNode) it).cst.toString())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Parameter name not found: " + InsnSupport.toString(node)));
  }

}
