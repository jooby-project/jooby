/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocStream.*;
import static io.jooby.internal.openapi.javadoc.JavaDocStream.tokens;
import static io.jooby.internal.openapi.javadoc.JavaDocStream.tree;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.TokenUtil;

public class JavaDocSupport {

  public static final Predicate<DetailAST> TYPES =
      tokens(
          TokenTypes.CLASS_DEF,
          TokenTypes.INTERFACE_DEF,
          TokenTypes.ENUM_DEF,
          TokenTypes.RECORD_DEF);

  public static final Predicate<DetailAST> EXTENDED_TYPES =
      TYPES.or(tokens(TokenTypes.VARIABLE_DEF, TokenTypes.PARAMETER_DEF));

  /**
   * Name from class, method, field, parameter.
   *
   * @param node Node.
   * @return Name.
   */
  public static String getSimpleName(DetailAST node) {
    checkTypeDef(EXTENDED_TYPES, node);
    return node.findFirstToken(TokenTypes.IDENT).getText();
  }

  public static String getClassName(DetailAST node) {
    checkTypeDef(TYPES, node);
    var classScope =
        Stream.concat(Stream.of(node), backward(node).filter(TYPES))
            .map(JavaDocSupport::getSimpleName)
            .toList();

    return Stream.concat(Stream.of(getPackageName(node)), classScope.stream())
        .collect(Collectors.joining("."));
  }

  public static DetailAST getCompilationUnit(DetailAST node) {
    return backward(node)
        .filter(tokens(TokenTypes.COMPILATION_UNIT))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Compilation unit missing: " + TokenUtil.getTokenName(node.getType())));
  }

  public static String getPackageName(DetailAST node) {
    return String.join(
        ".",
        tree(getCompilationUnit(node))
            .filter(tokens(TokenTypes.PACKAGE_DEF))
            .findFirst()
            .map(it -> tree(it).filter(tokens(TokenTypes.IDENT)).map(DetailAST::getText).toList())
            .orElse(List.of()));
  }

  public static String getQualifiedName(DetailAST node) {
    return tree(node.getFirstChild())
        .filter(tokens(TokenTypes.DOT).negate())
        .map(DetailAST::getText)
        .collect(Collectors.joining("."));
  }

  public static String toQualifiedName(DetailAST classDef, String typeName) {
    return switch (typeName) {
      case "char", "boolean", "int", "short", "long", "float", "double" -> typeName;
      case "Character" -> Character.class.getName();
      case "Boolean" -> Boolean.class.getName();
      case "Integer" -> Integer.class.getName();
      case "Short" -> Short.class.getName();
      case "Long" -> Long.class.getName();
      case "Float" -> Float.class.getName();
      case "Double" -> Double.class.getName();
      case "String" -> String.class.getName();
      default -> {
        checkTypeDef(TYPES, classDef);
        if (!typeName.contains(".")) {
          if (!getSimpleName(classDef).equals(typeName)) {
            var cu = getCompilationUnit(classDef);
            yield children(cu)
                .filter(tokens(TokenTypes.IMPORT))
                .map(
                    it ->
                        tree(it.getFirstChild())
                            .filter(tokens(TokenTypes.DOT).negate())
                            .map(DetailAST::getText)
                            .collect(Collectors.joining(".")))
                .filter(qualifiedName -> qualifiedName.endsWith("." + typeName))
                .findFirst()
                .orElseGet(() -> String.join(".", getPackageName(classDef), typeName));
          }
        }
        // Already qualified.
        yield typeName;
      }
    };
  }

  private static void checkTypeDef(Predicate<DetailAST> predicate, DetailAST node) {
    if (!predicate.test(node)) {
      throw new IllegalArgumentException(
          "Must be a type definition, found: " + TokenUtil.getTokenName(node.getType()));
    }
  }
}
