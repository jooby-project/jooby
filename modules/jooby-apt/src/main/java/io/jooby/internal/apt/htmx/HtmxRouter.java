/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.htmx;

import static io.jooby.internal.apt.CodeBlock.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import io.jooby.internal.apt.*;

public class HtmxRouter extends WebRouter<HtmxRoute> {

  private static final Set<String> HTMX_ANNOTATIONS =
      Set.of(
          "io.jooby.annotation.htmx.HxView",
          "io.jooby.annotation.htmx.HxOob",
          "io.jooby.annotation.htmx.HxOobs",
          "io.jooby.annotation.htmx.HxPushUrl",
          "io.jooby.annotation.htmx.HxRedirect",
          "io.jooby.annotation.htmx.HxRefresh",
          "io.jooby.annotation.htmx.HxSwap",
          "io.jooby.annotation.htmx.HxTarget",
          "io.jooby.annotation.htmx.HxTrigger",
          "io.jooby.annotation.htmx.HxTriggers");

  // The registry used to fuel the two-pass RestRouter bypass
  private final Set<String> claimedRoutes = new HashSet<>();

  public HtmxRouter(MvcContext context, TypeElement clazz) {
    super(context, clazz);
  }

  public static HtmxRouter parse(MvcContext context, TypeElement controller) {
    var router = new HtmxRouter(context, controller);

    for (var type : context.superTypes(controller)) {
      for (var enclosed : type.getEnclosedElements()) {
        if (enclosed.getKind() == ElementKind.METHOD) {
          var method = (ExecutableElement) enclosed;

          if (method.getModifiers().contains(Modifier.ABSTRACT)) {
            continue;
          }

          // 1. Identify HTMX endpoints
          if (isHtmxMethod(context, method)) {
            for (var annoMirror : method.getAnnotationMirrors()) {
              var annoElement = (TypeElement) annoMirror.getAnnotationType().asElement();

              if (HttpMethod.hasAnnotation(annoElement)) {
                var route = new HtmxRoute(router, method, annoElement);
                var uniqueKey = method.toString() + annoElement.getSimpleName();
                router.routes.putIfAbsent(uniqueKey, route);

                // 2. Claim the route for the two-pass pipeline!
                var httpMethod =
                    HttpMethod.findByAnnotationName(annoElement.getQualifiedName().toString());
                var paths = context.path(controller, method, annoElement);
                for (String path : paths) {
                  router.claimedRoutes.add(httpMethod + WebRoute.leadingSlash(path));
                }
              }
            }
          }
        }
      }
    }

    // 3. Resolve Overloads (identical to standard Jooby behavior)
    var grouped =
        router.routes.values().stream().collect(Collectors.groupingBy(HtmxRoute::getMethodName));
    for (var overloads : grouped.values()) {
      long distinctMethods =
          overloads.stream().map(r -> r.getMethod().toString()).distinct().count();
      if (distinctMethods > 1) {
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

  private static boolean isHtmxMethod(MvcContext ctx, ExecutableElement method) {
    boolean hasHtmxAnnotation =
        method.getAnnotationMirrors().stream()
            .map(am -> am.getAnnotationType().toString())
            .anyMatch(HTMX_ANNOTATIONS::contains);

    return hasHtmxAnnotation
        || Set.of(
                "io.jooby.htmx.HtmxResponse",
                "io.jooby.htmx.HtmxModelAndView",
                "io.jooby.ModelAndView",
                "io.jooby.MapModelAndView")
            .contains(
                new TypeDefinition(
                        ctx.getProcessingEnvironment().getTypeUtils(), method.getReturnType())
                    .getRawType()
                    .toString());
  }

  /** Exposes the paths this router has claimed so RestRouter can ignore them. */
  public Set<String> getClaimedRoutes() {
    return claimedRoutes;
  }

  @Override
  public String getGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName() + "Htmx");
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

    var routesList = getRoutes();
    for (int i = 0; i < routesList.size(); i++) {
      boolean isLast = i == routesList.size() - 1;
      for (String line : routesList.get(i).generateMapping(kt, generateTypeName, isLast)) {
        buffer.append(indent(6)).append(line);
      }
    }

    trimr(buffer);
    buffer
        .append(System.lineSeparator())
        .append(indent(4))
        .append("}")
        .append(System.lineSeparator())
        .append(System.lineSeparator());

    // 2. Generate the private handler methods containing our HtmxRoute logic
    var generatedHandlers = new HashSet<String>();
    for (var route : routesList) {
      if (generatedHandlers.add(route.getGeneratedName())) {
        for (String line : route.generateHandlerCall(kt)) {
          buffer.append(indent(4)).append(line);
        }
      }
    }

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
