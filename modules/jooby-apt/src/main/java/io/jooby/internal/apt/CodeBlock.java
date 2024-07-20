/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.List;
import java.util.stream.Stream;

import io.jooby.internal.apt.escape.EscapeUtils;

public class CodeBlock {
  public static String of(List<CharSequence> sequence) {
    return String.join("", sequence);
  }

  public static String statement(CharSequence... sequence) {
    return of(Stream.concat(Stream.of(sequence), Stream.of(System.lineSeparator())).toList());
  }

  public static String of(CharSequence... sequence) {
    return of(List.of(sequence));
  }

  public static CharSequence string(CharSequence value) {
    return "\"" + EscapeUtils.escapeJava(value) + "\"";
  }

  public static CharSequence clazz(boolean kt) {
    return kt ? "::class.java" : ".class";
  }

  public static CharSequence semicolon(boolean kt) {
    return kt ? "" : ";";
  }

  public static String indent(int count) {
    return " ".repeat(count);
  }

  public static String type(boolean kt, CharSequence value) {
    var result = value.toString().replace("java.lang.", "");
    if (kt) {
      return switch (result) {
        case "byte" -> "Byte";
        case "boolean" -> "Boolean";
        case "char", "Character" -> "Char";
        case "int", "Integer" -> "Int";
        case "short" -> "Short";
        case "long" -> "Long";
        case "double" -> "Double";
        case "float" -> "Float";
        case "Object" -> "Any";
        default -> {
          var arg = result.indexOf('<');
          var from = 0;
          var end = arg == -1 ? result.length() : arg;
          if (result.startsWith("java.util.")
              && Character.isUpperCase(result.charAt("java.util.".length()))) {
            // java.util.List => List
            from = "java.util.".length();
          }
          yield result.substring(from, end) + generics(true, result, arg);
        }
      };
    }
    return result;
  }

  private static String generics(boolean kt, String type, int i) {
    if (i == -1) {
      return "";
    }
    var buffer = new StringBuilder();
    buffer.append(type.charAt(i));
    var arg = new StringBuilder();
    for (int j = i + 1; j < type.length(); j++) {
      char ch = type.charAt(j);
      if (ch == '>' || ch == ',') {
        buffer.append(type(kt, arg));
        buffer.append(ch);
        if (ch == ',') {
          buffer.append(' ');
        }
        arg.setLength(0);
      } else if (!Character.isWhitespace(ch)) {
        arg.append(ch);
      }
    }
    return buffer.toString();
  }
}
