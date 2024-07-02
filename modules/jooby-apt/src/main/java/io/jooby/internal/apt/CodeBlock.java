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
        default -> result;
      };
    }
    return result;
  }
}
