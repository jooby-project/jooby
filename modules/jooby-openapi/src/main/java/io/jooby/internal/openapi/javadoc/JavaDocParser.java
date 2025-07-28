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

  private final JavaDocContext context;

  public JavaDocParser(JavaDocContext context) {
    this.context = context;
  }

  public Optional<ClassDoc> parse(Path filePath) throws Exception {
    ClassDoc result = null;
    var tree = context.resolve(filePath);
    for (var comment :
        forward(tree).filter(tokens(TokenTypes.COMMENT_CONTENT)).filter(HAS_CLASS).toList()) {
      var nodePath = path(comment);
      // ensure class
      if (result == null) {
        result = new ClassDoc(context, nodePath[1], comment.getParent());
      }
      if (nodePath[nodePath.length - 1] != null) {
        // there is a method here
        var method = new MethodDoc(context, nodePath[nodePath.length - 1], comment.getParent());
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
        forward(classDef.getParent())
            .filter(tokens(TokenTypes.PACKAGE_DEF))
            .findFirst()
            .orElse(null);
    var methodDef =
        backward(comment).filter(tokens(TokenTypes.METHOD_DEF)).findFirst().orElse(null);
    return new DetailAST[] {packageDef, classDef, methodDef};
  }
}
