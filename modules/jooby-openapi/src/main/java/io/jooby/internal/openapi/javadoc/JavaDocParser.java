/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocSupport.backward;
import static io.jooby.internal.openapi.javadoc.JavaDocSupport.tokens;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class JavaDocParser {

  public static Optional<ClassDoc> parse(Path filePath) throws CheckstyleException, IOException {
    ClassDoc result = null;
    var tree = JavaParser.parseFile(filePath.toFile(), JavaParser.Options.WITH_COMMENTS);
    for (var comment :
        JavaDocSupport.forward(tree).filter(tokens(TokenTypes.COMMENT_CONTENT)).toList()) {
      var nodePath = path(comment);
      // ensure class
      if (result == null) {
        result = new ClassDoc(nodePath[1], comment.getParent());
      }
      if (nodePath[nodePath.length - 1] != null) {
        // there is a method here
        var method = new MethodDoc(nodePath[nodePath.length - 1], comment.getParent());
        result.addMethod(method);
      }
    }
    return Optional.ofNullable(result);
  }

  private static DetailAST[] path(DetailAST comment) {
    var classDef =
        backward(comment)
            .filter(
                tokens(
                    TokenTypes.ENUM_DEF,
                    TokenTypes.CLASS_DEF,
                    TokenTypes.INTERFACE_DEF,
                    TokenTypes.RECORD_DEF))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("no type found"));
    var packageDef =
        JavaDocSupport.forward(classDef.getParent())
            .filter(tokens(TokenTypes.PACKAGE_DEF))
            .findFirst()
            .orElse(null);
    var methodDef =
        backward(comment).filter(tokens(TokenTypes.METHOD_DEF)).findFirst().orElse(null);
    return new DetailAST[] {packageDef, classDef, methodDef};
  }
}
