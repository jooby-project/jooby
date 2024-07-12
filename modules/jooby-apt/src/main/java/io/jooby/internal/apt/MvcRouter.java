/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.findAnnotationByName;
import static io.jooby.internal.apt.CodeBlock.indent;
import static io.jooby.internal.apt.CodeBlock.semicolon;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.*;
import javax.tools.JavaFileObject;

public class MvcRouter {
  private final MvcContext context;

  /** MVC router class. */
  private final TypeElement clazz;

  /** MVC route methods. */
  private final Map<String, MvcRoute> routes = new LinkedHashMap<>();

  public MvcRouter(MvcContext context, TypeElement clazz) {
    this.context = context;
    this.clazz = clazz;
  }

  public MvcRouter(TypeElement clazz, MvcRouter parent) {
    this.context = parent.context;
    this.clazz = clazz;
    for (var e : parent.routes.entrySet()) {
      this.routes.put(e.getKey(), new MvcRoute(context, this, e.getValue()));
    }
  }

  public boolean isKt() {
    return context
        .getProcessingEnvironment()
        .getElementUtils()
        .getAllAnnotationMirrors(getTargetType())
        .stream()
        .anyMatch(it -> it.getAnnotationType().asElement().toString().equals("kotlin.Metadata"));
  }

  public TypeElement getTargetType() {
    return clazz;
  }

  public String getGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName().toString());
  }

  public String getGeneratedFilename() {
    return getGeneratedType().replace('.', '/')
        + (isKt() ? ".kt" : JavaFileObject.Kind.SOURCE.extension);
  }

  public MvcRouter put(TypeElement httpMethod, ExecutableElement route) {
    var routeKey = route.toString();
    var existing = routes.get(routeKey);
    if (existing == null) {
      routes.put(routeKey, new MvcRoute(context, this, route).addHttpMethod(httpMethod));
    } else {
      if (existing.getMethod().getEnclosingElement().equals(getTargetType())) {
        existing.addHttpMethod(httpMethod);
      } else {
        // Favor override version of same method
        routes.put(routeKey, new MvcRoute(context, this, route).addHttpMethod(httpMethod));
      }
    }
    return this;
  }

  public List<MvcRoute> getRoutes() {
    return routes.values().stream().toList();
  }

  public boolean isAbstract() {
    return clazz.getModifiers().contains(Modifier.ABSTRACT);
  }

  public String getPackageName() {
    var classname = getGeneratedType();
    var pkgEnd = classname.lastIndexOf('.');
    return pkgEnd > 0 ? classname.substring(0, pkgEnd) : "";
  }

  /**
   * Generate the controller extension for MVC controller:
   *
   * <pre>{@code
   * public class Controller_ implements MvcExtension, MvcFactory {
   *     ....
   * }
   *
   * }</pre>
   *
   * @return
   */
  public String toSourceCode() throws IOException {
    var kt = isKt();
    var generateTypeName = context.generateRouterName(getTargetType().getSimpleName().toString());
    try (var in = getClass().getResourceAsStream("Source" + (kt ? ".kt" : ".java"))) {
      Objects.requireNonNull(in);
      var routes = this.routes.values();
      var suspended = routes.stream().filter(MvcRoute::isSuspendFun).toList();
      var noSuspended = routes.stream().filter(it -> !it.isSuspendFun()).toList();
      var buffer = new StringBuilder();
      context.generateStaticImports(
          this,
          (owner, fn) ->
              buffer.append(
                  CodeBlock.statement(
                      "import ", kt ? "" : "static ", owner, ".", fn, semicolon(kt))));
      var imports = buffer.toString();
      buffer.setLength(0);
      if (!suspended.isEmpty()) {
        buffer.append(CodeBlock.statement(indent(6), "val kooby = app as io.jooby.kt.Kooby"));
        buffer.append(CodeBlock.statement(indent(6), "kooby.coroutine {"));
        suspended.stream()
            .flatMap(it -> it.generateMapping(kt).stream())
            .forEach(line -> buffer.append(CodeBlock.indent(8)).append(line));
        trimr(buffer);
        buffer.append(System.lineSeparator()).append(CodeBlock.statement(indent(6), "}"));
      }
      noSuspended.stream()
          .flatMap(it -> it.generateMapping(kt).stream())
          .forEach(line -> buffer.append(CodeBlock.indent(6)).append(line));
      var bindings = trimr(buffer).toString();
      buffer.setLength(0);
      routes.stream()
          .flatMap(it -> it.generateHandlerCall(kt).stream())
          .forEach(line -> buffer.append(CodeBlock.indent(4)).append(line));
      return new String(in.readAllBytes(), StandardCharsets.UTF_8)
          .replace("${packageName}", getPackageName())
          .replace("${imports}", imports)
          .replace("${className}", getTargetType().getSimpleName())
          .replace("${generatedClassName}", generateTypeName)
          .replace("${constructors}", constructors(generateTypeName, kt))
          .replace("${bindings}", bindings)
          .replace("${methods}", trimr(buffer));
    }
  }

  private StringBuilder trimr(StringBuilder buffer) {
    var i = buffer.length() - 1;
    while (i > 0 && Character.isWhitespace(buffer.charAt(i))) {
      buffer.deleteCharAt(i);
      i = buffer.length() - 1;
    }
    return buffer;
  }

  private String constructors(String generatedName, boolean kt) {
    var injectAnnotations = Set.of("javax.inject.Inject", "jakarta.inject.Inject");
    var constructors =
        getTargetType().getEnclosedElements().stream()
            .filter(
                it ->
                    it.getKind() == ElementKind.CONSTRUCTOR
                        && it.getModifiers().contains(Modifier.PUBLIC))
            .map(ExecutableElement.class::cast)
            .toList();
    var targetType = getTargetType().getSimpleName();
    var buffer = new StringBuilder();
    var injectConstructor =
        constructors.stream()
            .filter(
                it ->
                    injectAnnotations.stream()
                        .anyMatch(annotation -> findAnnotationByName(it, annotation) != null))
            .findFirst()
            .orElse(null);
    final var defaultConstructor =
        constructors.stream().filter(it -> it.getParameters().isEmpty()).findFirst().orElse(null);
    if (injectConstructor != null) {
      constructor(
          generatedName,
          kt,
          buffer,
          List.of(),
          (output, params) -> {
            output
                .append("this(")
                .append(targetType)
                .append(kt ? "::class" : ".class")
                .append(")")
                .append(semicolon(kt))
                .append(System.lineSeparator());
          });
    } else {
      if (defaultConstructor != null) {
        constructor(
            generatedName,
            kt,
            buffer,
            List.of(),
            (output, params) -> {
              output
                  .append("this(")
                  .append(kt ? "" : "new ")
                  .append(targetType)
                  .append("())")
                  .append(semicolon(kt))
                  .append(System.lineSeparator());
            });
      }
    }
    var skip =
        Stream.of(injectConstructor, defaultConstructor)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    for (ExecutableElement constructor : constructors) {
      if (!skip.contains(constructor)) {
        constructor(
            generatedName,
            kt,
            buffer,
            constructor.getParameters().stream()
                .map(it -> Map.<Object, String>entry(it.asType(), it.getSimpleName().toString()))
                .toList(),
            (output, params) -> {
              var separator = ", ";
              output.append("this(").append(kt ? "" : "new ").append(targetType).append("(");
              params.forEach(e -> output.append(e.getValue()).append(separator));
              output.setLength(output.length() - separator.length());
              output.append("))").append(semicolon(kt)).append(System.lineSeparator());
            });
      }
    }

    if (injectConstructor != null) {
      if (kt) {
        constructor(
            generatedName,
            true,
            buffer,
            List.of(Map.entry("kotlin.reflect.KClass<" + targetType + ">", "type")),
            (output, params) -> {
              // this(java.util.function.Function<io.jooby.Context, ${className}> { ctx:
              // io.jooby.Context -> ctx.require<${className}>(type.java) })
              output
                  .append("this(java.util.function.Function<io.jooby.Context, ")
                  .append(targetType)
                  .append("> { ctx: io.jooby.Context -> ")
                  .append("ctx.require<")
                  .append(targetType)
                  .append(">(type.java)")
                  .append(" })")
                  .append(System.lineSeparator());
            });
      } else {
        constructor(
            generatedName,
            false,
            buffer,
            List.of(Map.entry("Class<" + targetType + ">", "type")),
            (output, params) -> {
              output
                  .append("this(")
                  .append("ctx -> ctx.require(type)")
                  .append(")")
                  .append(";")
                  .append(System.lineSeparator());
            });
      }
    }

    return System.lineSeparator() + indent(4) + buffer.toString().trim() + System.lineSeparator();
  }

  private void constructor(
      String generatedName,
      boolean kt,
      StringBuilder buffer,
      List<Map.Entry<Object, String>> parameters,
      BiConsumer<StringBuilder, List<Map.Entry<Object, String>>> body) {
    buffer.append(indent(4));
    if (kt) {
      buffer.append("constructor").append("(");
    } else {
      buffer.append("public ").append(generatedName).append("(");
    }
    var separator = ", ";
    parameters.forEach(
        e -> {
          if (kt) {
            buffer.append(e.getValue()).append(": ").append(e.getKey()).append(separator);
          } else {
            buffer.append(e.getKey()).append(" ").append(e.getValue()).append(separator);
          }
        });
    if (!parameters.isEmpty()) {
      buffer.setLength(buffer.length() - separator.length());
    }
    buffer.append(")");
    if (!kt) {
      buffer.append(" {").append(System.lineSeparator());
      buffer.append(indent(6));
    } else {
      buffer.append(" : ");
    }
    body.accept(buffer, parameters);
    if (!kt) {
      buffer.append(indent(4)).append("}");
    }
    buffer.append(System.lineSeparator()).append(System.lineSeparator());
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    var annotations = Optional.ofNullable(clazz.getAnnotationMirrors()).orElse(emptyList());
    annotations.forEach(
        annotation -> {
          buffer
              .append("@")
              .append(annotation.getAnnotationType().asElement().getSimpleName())
              .append("(");
          buffer.append(annotation.getElementValues()).append(") ");
        });
    buffer.append(clazz.asType().toString()).append(" {\n");
    routes.forEach(
        (httpMethod, route) -> {
          buffer.append("  ").append(route).append("\n");
        });
    buffer.append("}");
    return buffer.toString();
  }
}
