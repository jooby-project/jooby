/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static com.puppycrawl.tools.checkstyle.JavaParser.parseFile;
import static io.jooby.SneakyThrows.throwingFunction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.DetailAST;

public class JavaDocContext {
  private final Path baseDir;
  private final Map<Path, DetailAST> cache = new HashMap<>();

  public JavaDocContext(Path baseDir) {
    this.baseDir = baseDir;
  }

  public DetailAST resolve(Path path) {
    return cache.computeIfAbsent(
        baseDir.resolve(path),
        throwingFunction(
            filePath -> {
              if (Files.exists(filePath)) {
                return parseFile(filePath.toFile(), JavaParser.Options.WITH_COMMENTS);
              } else {
                return NULL;
              }
            }));
  }

  public static final DetailAST NULL =
      new DetailAST() {
        @Override
        public int getChildCount() {
          return 0;
        }

        @Override
        public int getChildCount(int type) {
          return 0;
        }

        @Override
        public DetailAST getParent() {
          return null;
        }

        @Override
        public String getText() {
          return "";
        }

        @Override
        public int getType() {
          return 0;
        }

        @Override
        public int getLineNo() {
          return 0;
        }

        @Override
        public int getColumnNo() {
          return 0;
        }

        @Override
        public DetailAST getLastChild() {
          return null;
        }

        @Override
        public boolean branchContains(int type) {
          return false;
        }

        @Override
        public DetailAST getPreviousSibling() {
          return null;
        }

        @Override
        public DetailAST findFirstToken(int type) {
          return null;
        }

        @Override
        public DetailAST getNextSibling() {
          return null;
        }

        @Override
        public DetailAST getFirstChild() {
          return null;
        }

        @Override
        public int getNumberOfChildren() {
          return 0;
        }

        @Override
        public boolean hasChildren() {
          return false;
        }
      };
}
