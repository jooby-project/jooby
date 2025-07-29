/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static com.puppycrawl.tools.checkstyle.JavaParser.parseFile;
import static io.jooby.SneakyThrows.throwingFunction;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.DetailAST;

public class JavaDocContext {
  private final List<Path> baseDir;
  private final Map<Path, DetailAST> cache = new HashMap<>();

  public JavaDocContext(Path baseDir) {
    this(List.of(baseDir));
  }

  public JavaDocContext(List<Path> baseDir) {
    this.baseDir = baseDir;
  }

  public DetailAST resolve(Path path) {
    return lookup(path)
        .map(
            it ->
                cache.computeIfAbsent(
                    it,
                    throwingFunction(
                        filePath -> {
                          return parseFile(filePath.toFile(), JavaParser.Options.WITH_COMMENTS);
                        })))
        .orElse(JavaDocNode.EMPTY_AST);
  }

  public DetailAST resolveType(Class<?> type) {
    return resolveType(type.getName());
  }

  public DetailAST resolveType(String typeName) {
    var segments = typeName.split("\\.");
    segments[segments.length - 1] = segments[segments.length - 1] + ".java";
    return resolve(Paths.get(String.join(File.separator, segments)));
  }

  protected Optional<Path> lookup(Path path) {
    return baseDir.stream()
        .map(parentDir -> parentDir.resolve(path))
        .filter(Files::exists)
        .findFirst();
  }
}
