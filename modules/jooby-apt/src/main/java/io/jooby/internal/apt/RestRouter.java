/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.CodeBlock.*;

import java.io.IOException;
import java.util.stream.Collectors;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class RestRouter extends WebRouter<RestRoute> {

  public RestRouter(MvcContext context, TypeElement clazz) {
    super(context, clazz);
  }

  public static RestRouter parse(MvcContext context, TypeElement controller) {
    RestRouter router = new RestRouter(context, controller);

    for (var enclosed : controller.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD) {
        ExecutableElement method = (ExecutableElement) enclosed;

        if (AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.JsonRpc") != null
            || AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.Trpc") != null) {
          continue;
        }

        for (var annoMirror : method.getAnnotationMirrors()) {
          TypeElement annoElement = (TypeElement) annoMirror.getAnnotationType().asElement();
          if (HttpMethod.hasAnnotation(annoElement)) {
            RestRoute route = new RestRoute(router, method, annoElement);
            router.routes.put(route.getMethodName() + annoElement.getSimpleName(), route);
          }
        }
      }
    }

    // Resolve Overloads
    var grouped =
        router.routes.values().stream().collect(Collectors.groupingBy(RestRoute::getMethodName));
    for (var overloads : grouped.values()) {
      if (overloads.size() > 1) {
        for (var route : overloads) {
          var paramsString =
              route.getRawParameterTypes(true, false).stream()
                  .map(it -> it.substring(Math.max(0, it.lastIndexOf(".") + 1)))
                  .map(it -> Character.toUpperCase(it.charAt(0)) + it.substring(1))
                  .collect(Collectors.joining());
          route.setGeneratedName(route.getMethodName() + paramsString);
        }
      }
    }
    return router;
  }

  @Override
  public String getGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName().toString());
  }

  @Override
  public String getSourceCode(Boolean generateKotlin) throws IOException {
    if (isEmpty()) return null;

    boolean kt = generateKotlin == Boolean.TRUE || isKt();
    var generateTypeName = getTargetType().getSimpleName().toString();
    var generatedClass = getGeneratedType().substring(getGeneratedType().lastIndexOf('.') + 1);

    var template = kt ? KOTLIN : JAVA;
    var suspended = getRoutes().stream().filter(WebRoute::isSuspendFun).toList();
    var noSuspended = getRoutes().stream().filter(it -> !it.isSuspendFun()).toList();
    var buffer = new StringBuilder();

    context.generateStaticImports(
        this,
        (owner, fn) ->
            buffer.append(
                statement("import ", kt ? "" : "static ", owner, ".", fn, semicolon(kt))));
    var imports = buffer.toString();
    buffer.setLength(0);

    if (kt) {
      buffer.append(indent(4)).append("@Throws(Exception::class)").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("override fun install(app: io.jooby.Jooby) {")
          .append(System.lineSeparator());
    } else {
      buffer
          .append(indent(4))
          .append("public void install(io.jooby.Jooby app) throws Exception {")
          .append(System.lineSeparator());
    }

    if (!suspended.isEmpty()) {
      buffer.append(statement(indent(6), "val kooby = app as io.jooby.kt.Kooby"));
      buffer.append(statement(indent(6), "kooby.coroutine {"));
      suspended.stream()
          .flatMap(
              it ->
                  it
                      .generateMapping(
                          kt,
                          generateTypeName,
                          suspended.indexOf(it) == suspended.size() - 1 && noSuspended.isEmpty())
                      .stream())
          .forEach(line -> buffer.append(CodeBlock.indent(8)).append(line));
      trimr(buffer);
      buffer.append(System.lineSeparator()).append(statement(indent(6), "}"));
    }

    noSuspended.stream()
        .flatMap(
            it ->
                it
                    .generateMapping(
                        kt, generateTypeName, noSuspended.indexOf(it) == noSuspended.size() - 1)
                    .stream())
        .forEach(line -> buffer.append(CodeBlock.indent(6)).append(line));

    trimr(buffer);
    buffer
        .append(System.lineSeparator())
        .append(indent(4))
        .append("}")
        .append(System.lineSeparator())
        .append(System.lineSeparator());

    getRoutes().stream()
        .flatMap(it -> it.generateHandlerCall(kt).stream())
        .forEach(line -> buffer.append(CodeBlock.indent(4)).append(line));

    return template
        .replace("${packageName}", getPackageName())
        .replace("${imports}", imports)
        .replace("${className}", generateTypeName)
        .replace("${generatedClassName}", generatedClass)
        .replace("${implements}", "io.jooby.Extension")
        .replace("${constructors}", constructors(generatedClass, kt))
        .replace("${methods}", trimr(buffer));
  }
}
