/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class JavaDocParser {

  private static final Predicate<DetailAST> HAS_CLASS =
      it -> backward(it).anyMatch(tokens(TokenTypes.CLASS_DEF));

  private static final Predicate<DetailAST> STATEMENT_LIST =
      it -> backward(it).anyMatch(tokens(TokenTypes.SLIST));

  private final JavaDocContext context;

  public JavaDocParser(JavaDocContext context) {
    this.context = context;
  }

  public Optional<ClassDoc> parseMvc(Path filePath) throws Exception {
    ClassDoc result = null;
    var tree = context.resolve(filePath);
    for (var comment :
        forward(tree)
            .filter(tokens(TokenTypes.COMMENT_CONTENT))
            .filter(HAS_CLASS)
            .filter(STATEMENT_LIST.negate())
            .toList()) {
      var classOrMethod = classOrMethod(comment);
      if (classOrMethod.getType() == TokenTypes.METHOD_DEF) {
        if (result == null) {
          // No comment on class
          result =
              new ClassDoc(
                  context,
                  tree(tree)
                      .filter(
                          tokens(
                              TokenTypes.ENUM_DEF,
                              TokenTypes.CLASS_DEF,
                              TokenTypes.INTERFACE_DEF,
                              TokenTypes.RECORD_DEF))
                      .findFirst()
                      .orElseThrow(() -> new IllegalStateException("Class not found " + tree)),
                  JavaDocNode.EMPTY_AST);
        }
        var method = new MethodDoc(context, classOrMethod, comment.getParent());
        result.addMethod(method);
      } else {
        // always as class
        result = new ClassDoc(context, classOrMethod, comment.getParent());
      }
    }
    return Optional.ofNullable(result);
  }

  private static DetailAST classOrMethod(DetailAST comment) {
    return backward(comment)
        .filter(
            tokens(
                TokenTypes.METHOD_DEF,
                TokenTypes.ENUM_DEF,
                TokenTypes.CLASS_DEF,
                TokenTypes.INTERFACE_DEF,
                TokenTypes.RECORD_DEF))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Invalid comment: " + comment.getText()));
  }
}
