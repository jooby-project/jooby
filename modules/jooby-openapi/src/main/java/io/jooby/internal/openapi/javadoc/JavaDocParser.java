/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static com.puppycrawl.tools.checkstyle.JavaParser.parseFile;
import static io.jooby.SneakyThrows.throwingFunction;
import static io.jooby.internal.openapi.javadoc.JavaDocStream.*;
import static io.jooby.internal.openapi.javadoc.JavaDocStream.tokens;
import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;
import static java.util.Optional.ofNullable;

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
import com.puppycrawl.tools.checkstyle.utils.TokenUtil;
import com.puppycrawl.tools.checkstyle.utils.XpathUtil;
import io.jooby.Context;
import io.jooby.Router;

public class JavaDocParser {

  public static final JavaDocParser NOOP = new JavaDocParser(List.of());

  private record ScriptRef(String operationId, DetailAST comment) {}

  private final List<Path> baseDir;
  private final Map<Path, DetailAST> cache = new HashMap<>();

  public JavaDocParser(Path baseDir) {
    this(List.of(baseDir));
  }

  public JavaDocParser(List<Path> baseDir) {
    this.baseDir = baseDir;
  }

  public Optional<ClassDoc> parse(String typeName) {
    return ofNullable(traverse(resolveType(typeName)).get(typeName));
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

  private Map<String, ClassDoc> traverse(DetailAST tree) {
    var classes = new HashMap<String, ClassDoc>();
    traverse(
        List.of(tree),
        TYPES,
        modifiers -> tree(modifiers).noneMatch(tokens(TokenTypes.LITERAL_PRIVATE)),
        (scope, comment) -> {
          var scopes = typeHierarchy(scope);
          var counter = new AtomicInteger(0);
          counter.addAndGet(comment == JavaDocNode.EMPTY_AST ? 0 : 1);
          var classDoc = new ClassDoc(this, scope, comment);

          // MVC routes
          traverse(
              scopes,
              tokens(TokenTypes.VARIABLE_DEF, TokenTypes.METHOD_DEF),
              modifiers -> tree(modifiers).noneMatch(tokens(TokenTypes.LITERAL_STATIC)),
              (member, memberComment) -> {
                counter.addAndGet(memberComment == JavaDocNode.EMPTY_AST ? 0 : 1);
                // check member belong to current scope
                if (backward(member)
                    .filter(TYPES)
                    .findFirst()
                    .filter(scopes::contains)
                    .isPresent()) {
                  if (member.getType() == TokenTypes.VARIABLE_DEF) {
                    classDoc.addField(new FieldDoc(this, member, memberComment));
                  } else {
                    classDoc.addMethod(new MethodDoc(this, member, memberComment));
                  }
                }
              });
          // Script routes
          counter.addAndGet(scripts(scope, classDoc, null, null, new HashSet<>()));

          if (counter.get() > 0) {
            classes.put(classDoc.getName(), classDoc);
          }
        });
    return classes;
  }

  private void traverse(
      List<DetailAST> scopes,
      Predicate<DetailAST> types,
      Predicate<DetailAST> modifiers,
      BiConsumer<DetailAST, DetailAST> action) {

    for (var scope : scopes) {
      for (var node : tree(scope).filter(types).toList()) {
        var mods =
            tree(node)
                .filter(tokens(TokenTypes.MODIFIERS))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Modifiers not found on " + TokenUtil.getTokenName(node.getType())));
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
  }

  private List<DetailAST> typeHierarchy(DetailAST scope) {
    var result = new ArrayList<DetailAST>();
    // call parent members first, so we can inherit and override later.
    children(scope)
        .filter(tokens(TokenTypes.EXTENDS_CLAUSE))
        .findFirst()
        .map(it -> JavaDocSupport.toQualifiedName(scope, getQualifiedName(it)))
        .flatMap(
            superClass ->
                tree(resolveType(superClass))
                    .filter(TYPES)
                    .filter(it -> superClass.endsWith("." + getSimpleName(it)))
                    .findFirst())
        .ifPresent(parent -> result.addAll(typeHierarchy(parent)));
    result.add(scope);
    return result;
  }

  private int scripts(
      DetailAST scope, ClassDoc classDoc, PathDoc pathDoc, String prefix, Set<DetailAST> visited) {
    var counter = new AtomicInteger(0);
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
                .orElseThrow(() -> new IllegalArgumentException("No method call found: " + script));
        var scriptComment =
            children(script)
                .filter(tokens(TokenTypes.BLOCK_COMMENT_BEGIN))
                .findFirst()
                .orElse(JavaDocNode.EMPTY_AST);
        if (scriptComment != JavaDocNode.EMPTY_AST) {
          counter.incrementAndGet();
        }
        if (Router.METHODS.contains(callName.toUpperCase())) {
          pathLiteral(script)
              .ifPresent(
                  pattern -> {
                    var resolvedComment = resolveScriptComment(classDoc, script, scriptComment);
                    if (scriptComment != resolvedComment.comment) {
                      counter.incrementAndGet();
                    }
                    var scriptDoc =
                        new ScriptDoc(
                            this,
                            callName.toUpperCase(),
                            computePath(prefix, pattern),
                            script,
                            resolvedComment.comment);
                    if (resolvedComment.operationId() != null) {
                      scriptDoc.setOperationId(resolvedComment.operationId());
                    }
                    scriptDoc.setPath(pathDoc);
                    classDoc.addScript(scriptDoc);
                  });
        } else if ("path".equals(callName)) {
          pathLiteral(script)
              .ifPresent(
                  path -> {
                    scripts(
                        script,
                        classDoc,
                        new PathDoc(this, script, scriptComment),
                        computePath(prefix, path),
                        visited);
                  });
        }
      }
    }
    return counter.get();
  }

  /**
   * get("/reference", this::findPetById); post("/static-reference",
   * javadoc.input.LambdaRefApp::staticFindPetById); put("/external-reference",
   * RequestHandler::external); get("/external-subPackage-reference",
   * SubPackageHandler::subPackage);
   *
   * @param classDoc
   * @param script
   * @param defaultComment
   * @return
   */
  private ScriptRef resolveScriptComment(
      ClassDoc classDoc, DetailAST script, DetailAST defaultComment) {
    // ELIST -> LAMBDA (children)
    // ELIST -> EXPR -> METHOD_REF (tree)
    return children(script)
        .filter(tokens(TokenTypes.ELIST))
        .findFirst()
        .map(
            statementList ->
                children(statementList)
                    .filter(tokens(TokenTypes.LAMBDA))
                    .findFirst()
                    .map(lambda -> new ScriptRef(null, defaultComment))
                    .orElseGet(
                        () ->
                            tree(statementList)
                                .filter(tokens(TokenTypes.METHOD_REF))
                                .findFirst()
                                .flatMap(ref -> ofNullable(resolveFromMethodRef(classDoc, ref)))
                                .orElseGet(() -> new ScriptRef(null, defaultComment))))
        .orElseGet(() -> new ScriptRef(null, defaultComment));
  }

  private ScriptRef resolveFromMethodRef(ClassDoc classDoc, DetailAST methodRef) {
    var referenceOwner = getQualifiedName(methodRef);
    DetailAST scope = null;
    String className;
    if (referenceOwner.equals("this")) {
      scope = classDoc.getNode();
      className = classDoc.getName();
    } else {
      // resolve className
      className = toQualifiedName(classDoc.node, referenceOwner);
      scope = resolveType(className);
      if (scope == JavaDocNode.EMPTY_AST) {
        // not found
        return null;
      }
    }
    var methodName =
        children(methodRef).filter(tokens(TokenTypes.IDENT)).toList().getLast().getText();
    var method =
        tree(scope)
            .filter(tokens(TokenTypes.METHOD_DEF))
            .filter(
                it ->
                    children(it)
                        .filter(tokens(TokenTypes.IDENT))
                        .findFirst()
                        .filter(e -> e.getText().equals(methodName))
                        .isPresent())
            // One Argument
            .filter(it -> tree(it).filter(tokens(TokenTypes.PARAMETER_DEF)).count() == 1)
            // Context Type
            .filter(
                it ->
                    tree(it)
                        .filter(tokens(TokenTypes.PARAMETER_DEF))
                        .findFirst()
                        .flatMap(p -> children(p).filter(tokens(TokenTypes.TYPE)).findFirst())
                        .filter(
                            type ->
                                toQualifiedName(classDoc.node, getQualifiedName(type))
                                    .equals(Context.class.getName()))
                        .isPresent())
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No method found: " + className + "." + methodName));
    return children(method)
        .filter(tokens(TokenTypes.MODIFIERS))
        .findFirst()
        .flatMap(it -> children(it).filter(tokens(TokenTypes.BLOCK_COMMENT_BEGIN)).findFirst())
        .map(comment -> new ScriptRef(methodName, comment))
        .orElseGet(() -> new ScriptRef(null, JavaDocNode.EMPTY_AST));
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

  private DetailAST resolveType(String typeName) {
    var segments = typeName.split("\\.");
    segments[segments.length - 1] = segments[segments.length - 1] + ".java";
    return resolve(Paths.get(String.join(File.separator, segments)));
  }

  private Optional<Path> lookup(Path path) {
    return baseDir.stream()
        .map(parentDir -> parentDir.resolve(path))
        .filter(Files::exists)
        .findFirst();
  }
}
