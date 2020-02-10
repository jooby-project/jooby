package io.jooby.internal.openapi;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ASMType {
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

  public static String parse(String signature) {
    return parse(signature, type -> true);
  }

  public static String parse(String signature, Predicate<String> filter) {
    String primitive = primitive(signature);
    if (primitive != null) {
      return primitive;
    }
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

  private static String primitive(String value) {
    switch (value) {
      case "Z": return "boolean";
      case "C": return "char";
      case "B": return "byte";
      case "S": return "short";
      case "I": return "int";
      case "J": return "long";
      case "F": return "float";
      case "D": return "double";
      case "V": return "void";
    }
    return null;
  }

}
