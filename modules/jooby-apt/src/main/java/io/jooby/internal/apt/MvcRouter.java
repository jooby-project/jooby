/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.findAnnotationByName;
import static io.jooby.internal.apt.StringCodeBlock.indent;
import static io.jooby.internal.apt.StringCodeBlock.semicolon;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.*;
import io.jooby.apt.MvcContext;

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
    return context.getProcessingEnvironment().getOptions().containsKey("jooby.kt")
        || context
            .getProcessingEnvironment()
            .getElementUtils()
            .getAllAnnotationMirrors(getTargetType())
            .stream()
            .anyMatch(
                it -> it.getAnnotationType().asElement().toString().equals("kotlin.Metadata"));
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
                  StringCodeBlock.statement("import static ", owner, ".", fn, semicolon(kt))));
      var imports = buffer.toString();
      buffer.setLength(0);
      if (!suspended.isEmpty()) {
        buffer.append(
            StringCodeBlock.statement(indent(6), "val coroutineRouter = app as io.jooby.kt.Kooby"));
        buffer.append(StringCodeBlock.statement(indent(6), "coroutineRouter.coroutine {"));
        suspended.stream()
            .flatMap(it -> it.generateMapping(kt).stream())
            .forEach(line -> buffer.append(StringCodeBlock.indent(8)).append(line));
        buffer.append(StringCodeBlock.statement(System.lineSeparator(), indent(6), "}"));
      }
      noSuspended.stream()
          .flatMap(it -> it.generateMapping(kt).stream())
          .forEach(line -> buffer.append(StringCodeBlock.indent(6)).append(line));
      var bindings = buffer.toString();
      buffer.setLength(0);
      routes.stream()
          .flatMap(it -> it.generateHandlerCall(kt).stream())
          .forEach(line -> buffer.append(StringCodeBlock.indent(4)).append(line));
      return new String(in.readAllBytes(), StandardCharsets.UTF_8)
          .replace("${packageName}", getPackageName())
          .replace("${imports}", imports)
          .replace("${className}", getTargetType().getSimpleName())
          .replace("${generatedClassName}", generateTypeName)
          .replace("${defaultInstance}", defaultInstance(kt))
          .replace("${bindings}", bindings)
          .replace("${routes}", buffer);
    }
  }

  private String defaultInstance(boolean kt) {
    var injectAnnotations = Set.of("javax.inject.Inject", "jakarta.inject.Inject");
    var constructors =
        getTargetType().getEnclosedElements().stream()
            .filter(it -> it.getKind() == ElementKind.CONSTRUCTOR)
            .toList();
    var hasDefaultConstructor =
        constructors.stream()
            .map(ExecutableElement.class::cast)
            .anyMatch(
                it -> it.getParameters().isEmpty() && it.getModifiers().contains(Modifier.PUBLIC));
    var inject =
        constructors.stream()
            .anyMatch(
                it ->
                    injectAnnotations.stream()
                        .anyMatch(annotation -> findAnnotationByName(it, annotation) != null));
    if (inject || !hasDefaultConstructor) {
      return getTargetType().getSimpleName() + (kt ? "::class" : ".class");
    } else {
      var newInstance = getTargetType().getSimpleName() + "()";
      return kt ? newInstance : "new " + newInstance;
    }
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
