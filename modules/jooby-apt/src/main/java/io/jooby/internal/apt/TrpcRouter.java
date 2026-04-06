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

public class TrpcRouter extends WebRouter<TrpcRoute> {

  public TrpcRouter(MvcContext context, TypeElement clazz) {
    super(context, clazz);
  }

  public static TrpcRouter parse(MvcContext context, TypeElement controller) {
    var router = new TrpcRouter(context, controller);

    // 1. Walk up the inheritance tree to catch tRPC methods in base classes
    for (TypeElement type : context.superTypes(controller)) {
      for (var enclosed : type.getEnclosedElements()) {
        if (enclosed.getKind() == ElementKind.METHOD) {
          ExecutableElement method = (ExecutableElement) enclosed;

          // Ignore abstract methods
          if (method.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT)) {
            continue;
          }

          if (AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.trpc.Trpc")
                  != null
              || AnnotationSupport.findAnnotationByName(
                      method, "io.jooby.annotation.trpc.Trpc.Query")
                  != null
              || AnnotationSupport.findAnnotationByName(
                      method, "io.jooby.annotation.trpc.Trpc.Mutation")
                  != null) {

            TrpcRoute route = new TrpcRoute(router, method);

            // 2. Use the full string signature to prevent overloaded methods from clobbering each
            // other
            String uniqueKey = method.toString();

            // 3. putIfAbsent ensures subclass overrides take priority over base class methods
            router.routes.putIfAbsent(uniqueKey, route);
          }
        }
      }
    }

    // Resolve Overloads
    var grouped =
        router.routes.values().stream().collect(Collectors.groupingBy(TrpcRoute::getMethodName));
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
    return context.generateRouterName(getTargetType().getQualifiedName() + "Trpc");
  }

  @Override
  public String toSourceCode(boolean kt) throws IOException {
    var generateTypeName = getTargetType().getSimpleName().toString();
    var generatedClass = getGeneratedType().substring(getGeneratedType().lastIndexOf('.') + 1);

    var template = getTemplate(kt);
    var buffer = new StringBuilder();

    context.generateStaticImports(
        this,
        (owner, fn) ->
            buffer.append(
                statement("import ", kt ? "" : "static ", owner, ".", fn, semicolon(kt))));
    var imports = buffer.toString();
    buffer.setLength(0);

    // begin install
    if (kt) {
      buffer.append(indent(4)).append("@Throws(Exception::class)").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("override fun install(path: String, app: io.jooby.Jooby) {")
          .append(System.lineSeparator());
    } else {
      buffer
          .append(indent(4))
          .append("public void install(String path, io.jooby.Jooby app) throws Exception {")
          .append(System.lineSeparator());
    }

    getRoutes().stream()
        .flatMap(it -> it.generateMapping(kt, generateTypeName).stream())
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
        .replace("${implements}", "io.jooby.trpc.TrpcService")
        .replace("${constructors}", constructors(generatedClass, kt))
        .replace("${methods}", trimr(buffer));
  }
}
