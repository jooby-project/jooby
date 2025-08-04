/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static com.puppycrawl.tools.checkstyle.JavaParser.parseFile;
import static io.jooby.SneakyThrows.throwingFunction;
import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;
import static io.jooby.internal.openapi.javadoc.JavaDocSupport.tokens;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.XpathUtil;
import io.jooby.Router;

public class JavaDocParser {

  private final List<Path> baseDir;
  private final Map<Path, DetailAST> cache = new HashMap<>();

  public JavaDocParser(Path baseDir) {
    this(List.of(baseDir));
  }

  public JavaDocParser(List<Path> baseDir) {
    this.baseDir = baseDir;
  }

  public Optional<ClassDoc> parse(String typeName) {
    return Optional.ofNullable(traverse(resolveType(typeName)).get(typeName));
  }

  public Map<String, ClassDoc> traverse(DetailAST tree) {
    var classes = new HashMap<String, ClassDoc>();
    var types =
        tokens(
            TokenTypes.ENUM_DEF,
            TokenTypes.CLASS_DEF,
            TokenTypes.INTERFACE_DEF,
            TokenTypes.RECORD_DEF);
    traverse(
        tree,
        types,
        modifiers -> tree(modifiers).noneMatch(tokens(TokenTypes.LITERAL_PRIVATE)),
        (scope, comment) -> {
          var counter = new AtomicInteger(0);
          counter.addAndGet(comment == JavaDocNode.EMPTY_AST ? 0 : 1);
          var classDoc = new ClassDoc(this, scope, comment);

          // MVC routes
          traverse(
              scope,
              tokens(TokenTypes.VARIABLE_DEF, TokenTypes.METHOD_DEF),
              modifiers -> tree(modifiers).noneMatch(tokens(TokenTypes.LITERAL_STATIC)),
              (member, memberComment) -> {
                counter.addAndGet(memberComment == JavaDocNode.EMPTY_AST ? 0 : 1);
                // check member belong to current scope
                if (scope == backward(member).filter(types).findFirst().orElse(null)) {
                  if (member.getType() == TokenTypes.VARIABLE_DEF) {
                    classDoc.addField(new FieldDoc(this, member, memberComment));
                  } else {
                    classDoc.addMethod(new MethodDoc(this, member, memberComment));
                  }
                }
              });
          // Script routes
          scripts(scope, null, null, new HashSet<>(), classDoc);

          if (counter.get() > 0) {
            classes.put(classDoc.getName(), classDoc);
          }
        });
    return classes;
  }

  private void scripts(
      DetailAST scope, PathDoc pathDoc, String prefix, Set<DetailAST> visited, ClassDoc classDoc) {
    for (var script : tree(scope).filter(tokens(TokenTypes.METHOD_CALL)).toList()) {
      if (visited.add(script)) {
        // Test for HTTP method name
        var callName =
            tree(script)
                .filter(tokens(TokenTypes.IDENT))
                .findFirst()
                .map(DetailAST::getText)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No method call found: " + script));
        var scriptComment =
            children(script)
                .filter(tokens(TokenTypes.BLOCK_COMMENT_BEGIN))
                .findFirst()
                .orElse(JavaDocNode.EMPTY_AST);
        if (Router.METHODS.contains(callName.toUpperCase())) {
          pathLiteral(script)
              .ifPresent(
                  pattern -> {
                    var scriptDoc =
                        new ScriptDoc(
                            this,
                            callName.toUpperCase(),
                            computePath(prefix, pattern),
                            script,
                            scriptComment);
                    scriptDoc.setPath(pathDoc);
                    classDoc.addScript(scriptDoc);
                  });
        } else if ("path".equals(callName)) {
          pathLiteral(script)
              .ifPresent(
                  path -> {
                    scripts(
                        script,
                        new PathDoc(this, script, scriptComment),
                        computePath(prefix, path),
                        visited,
                        classDoc);
                  });
        }
      }
    }
  }

  /**
   * ELIST -> EXPR -> STRING_LITERAL
   *
   * @param script Get string literal from method call.
   * @return String literal.
   */
  private static Optional<String> pathLiteral(DetailAST script) {
    return children(script)
        .filter(tokens(TokenTypes.ELIST))
        .findFirst()
        .flatMap(it -> children(it).filter(tokens(TokenTypes.EXPR)).findFirst())
        .flatMap(it -> children(it).filter(tokens(TokenTypes.STRING_LITERAL)).findFirst())
        .map(XpathUtil::getTextAttributeValue);
  }

  private String computePath(String prefix, String pattern) {
    if (prefix == null) {
      return Router.normalizePath(pattern);
    }
    return Router.noTrailingSlash(Router.normalizePath(prefix + pattern));
  }

  private void traverse(
      DetailAST tree,
      Predicate<DetailAST> types,
      Predicate<DetailAST> modifiers,
      BiConsumer<DetailAST, DetailAST> action) {
    for (var node : tree(tree).filter(types).toList()) {
      var mods =
          tree(node)
              .filter(tokens(TokenTypes.MODIFIERS))
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Modifiers not found on " + node));
      if (modifiers.test(mods)) {
        var docRoot = node.getType() == TokenTypes.VARIABLE_DEF ? mods.getParent() : mods;
        var comment =
            tree(docRoot)
                .filter(tokens(TokenTypes.BLOCK_COMMENT_BEGIN))
                .findFirst()
                .orElse(JavaDocNode.EMPTY_AST);
        action.accept(node, comment);
      }
    }
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

  private DetailAST resolveType(String typeName) {
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
