/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.javadoc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class JavadocTestHelper {

  public static ParseResult parseCode(String source) throws Exception {
    Path tempFile = Files.createTempFile("JoobyTestStub", ".java");
    Files.writeString(tempFile, source, StandardCharsets.UTF_8);

    try {
      File file = tempFile.toFile();
      FileText text = new FileText(file, Arrays.asList(source.split("\\r?\\n")));
      FileContents contents = new FileContents(text);

      // CRITICAL: We must use WITH_COMMENTS, otherwise Checkstyle drops the Javadocs!
      DetailAST rootAst = JavaParser.parseFile(file, JavaParser.Options.WITH_COMMENTS);

      return new ParseResult(contents, rootAst);
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  public static DetailAST findToken(DetailAST root, int tokenType) {
    if (root == null) return null;
    if (root.getType() == tokenType) return root;

    DetailAST child = root.getFirstChild();
    while (child != null) {
      DetailAST match = findToken(child, tokenType);
      if (match != null) return match;
      child = child.getNextSibling();
    }
    return null;
  }

  public record ParseResult(FileContents contents, DetailAST rootAst) {
    public DetailAST getMethodNode() {
      return findToken(rootAst, TokenTypes.METHOD_DEF);
    }
  }
}
