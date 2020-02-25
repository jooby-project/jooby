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

public class RequestParser {

  private static class WebArgument {
    String javaType;

    Boolean required;

    Boolean single;

    Object defaultValue;

    public void set(ParameterExt argument) {
      Optional.ofNullable(javaType).ifPresent(argument::setJavaType);
      Optional.ofNullable(required).ifPresent(argument::setRequired);
      Optional.ofNullable(single).ifPresent(argument::setSingle);
      Optional.ofNullable(defaultValue).ifPresent(argument::setDefaultValue);
    }

    public void set(RequestBodyExt argument) {
      Optional.ofNullable(javaType).ifPresent(argument::setJavaType);
      Optional.ofNullable(required).ifPresent(argument::setRequired);
    }
  }

  public static Optional<RequestBodyExt> requestBody(MethodNode node) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(node.instructions.iterator(),
            Spliterator.ORDERED),
        false)
        .filter(MethodInsnNode.class::isInstance)
        .map(MethodInsnNode.class::cast)
        .filter(i -> i.owner.equals(TypeFactory.CONTEXT.getInternalName()) && i.name.equals("body"))
        .findFirst()
        .map(i -> {
          Signature signature = Signature.create(i);
          if (signature.matches(Class.class)) {
            String bodyType = valueType(i)
                .orElseThrow(() -> new IllegalStateException(
                    "Body type not found, for: " + InsnSupport.toString(i)));
            RequestBodyExt body = new RequestBodyExt();
            body.setJavaType(bodyType);
            return body;
          } else {
            RequestBodyExt body = new RequestBodyExt();
            argumentType("body", i).set(body);
            return body;
          }
        });
  }

  public static List<ParameterExt> parameters(MethodNode node) {
    List<MethodInsnNode> nodes = StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(node.instructions.iterator(), Spliterator.ORDERED),
        false)
        .filter(MethodInsnNode.class::isInstance)
        .map(MethodInsnNode.class::cast)
        .filter(i -> i.owner.equals("io/jooby/Context"))
        .collect(Collectors.toList());
    List<ParameterExt> args = new ArrayList<>();
    for (MethodInsnNode methodInsnNode : nodes) {
      Signature signature = Signature.create(methodInsnNode);
      ParameterExt argument = new ParameterExt();
      if (signature.matches("path")) {
        argument.setIn("path");
        if (signature.matches(String.class)) {
          argument.setName(argumentName(methodInsnNode));
          argumentType(argument.getName(), methodInsnNode).set(argument);
        } else if (signature.matches(Class.class)) {
          argument.setName(signature.getMethod());
          argumentContextToType(argument, methodInsnNode);
        } else {
          // Unsupported path usage
        }
      } else if (signature.matches("query")) {
        argument.setIn("query");
        if (signature.matches(String.class)) {
          argument.setName(argumentName(methodInsnNode));
          argumentType(argument.getName(), methodInsnNode).set(argument);
        } else if (signature.matches(Class.class)) {
          argument.setName(signature.getMethod());
          argumentContextToType(argument, methodInsnNode);
        } else {
          // Unsupported query usage
        }
      } else if (signature.matches("form") || signature.matches("multipart")) {
        argument.setIn("form");
        if (signature.matches(String.class)) {
          argument.setName(argumentName(methodInsnNode));
          argumentType(argument.getName(), methodInsnNode).set(argument);
        } else if (signature.matches(Class.class)) {
          argument.setName(signature.getMethod());
          argumentContextToType(argument, methodInsnNode);
        } else {
          // Unsupported form usage
        }
      }

      if (argument.getJavaType() != null) {
        args.add(argument);
      } else {
        // Unsupported parameter usage
      }
    }
    return args;
  }

  private static void argumentContextToType(ParameterExt argument, MethodInsnNode node) {
    String type = valueType(node)
        .orElseThrow(() -> new IllegalStateException(
            "Parameter type not found, for: " + argument.getName()));
    argument.setJavaType(type);
    argument.setSingle(false);
  }

  private static Optional<String> valueType(MethodInsnNode node) {
    return InsnSupport.prev(node)
        .filter(LdcInsnNode.class::isInstance)
        .findFirst()
        .map(LdcInsnNode.class::cast)
        .filter(i -> i.cst instanceof Type)
        .map(i -> (Type) i.cst)
        .map(Type::getClassName);
  }

  private static WebArgument argumentType(String argumentName, MethodInsnNode node) {
    MethodInsnNode convertCall = InsnSupport.next(node)
        .filter(valueOwner())
        .map(MethodInsnNode.class::cast)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Parameter type not found, for: " + argumentName));
    Signature convert = Signature.create(convertCall);
    WebArgument argument = new WebArgument();
    if (convert.matches("value") || convert.matches("valueOrNull") || convert.getMethod()
        .endsWith("Value")) {
      argument.javaType = Type.getReturnType(convertCall.desc).getClassName();
      if (convert.matches("valueOrNull")) {
        argument.required = false;
      } else {
        if (convert.getParameterCount() == 0) {
          argument.required = true;
        } else {
          argument.required = false;
          argument.defaultValue = argumentDefaultValue(convertCall.getPrevious());
        }
      }
    } else if (convert.matches("toList")) {
      argument.javaType = toGenericOne(convertCall, convert, List.class);
      argument.required = false;
    } else if (convert.matches("toSet")) {
      argument.javaType = toGenericOne(convertCall, convert, Set.class);
      argument.required = false;
    } else if (convert.matches("toOptional")) {
      argument.javaType = toGenericOne(convertCall, convert, Optional.class);
      argument.required = false;
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
                    argument.defaultValue = argumentDefaultValue(elseCall.getPrevious());
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
      argument.javaType = toType.getClassName();
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
      argument.javaType = toType.getClassName();
      argument.required = true;
    } else if (convert.matches("toMap")) {
      argument.javaType = "java.util.Map<java.lang.String,java.lang.String>";
      argument.required = true;
      argument.single = false;
    } else if (convert.matches("toMultimap")) {
      argument.javaType = "java.util.Map<java.lang.String,java.util.List<java.lang.String>>";
      argument.required = true;
      argument.single = false;
    } else {
      throw new IllegalStateException("Unhandle parameter type: " + convert);
    }
    return argument;
  }

  private static Predicate<AbstractInsnNode> valueOwner() {
    return e -> {
      if (e instanceof MethodInsnNode) {
        return ((MethodInsnNode) e).owner.equals("io/jooby/Value")
            || ((MethodInsnNode) e).owner.equals("io/jooby/ValueNode")
            || ((MethodInsnNode) e).owner.equals("io/jooby/Body");
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

  private static String toGenericOne(MethodInsnNode node, Signature signature,
      Class collectionType) {
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
