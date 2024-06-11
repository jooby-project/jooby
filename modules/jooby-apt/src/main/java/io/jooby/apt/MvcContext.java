/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import io.jooby.apt.JoobyProcessor.Options;
import io.jooby.internal.apt.*;

/**
 * Utility class which give access to {@link ProcessingEnvironment} mainly, few utility methods and
 * processor options.
 */
public class MvcContext {
  private final ProcessingEnvironment processingEnvironment;
  private final boolean debug;
  private final boolean incremental;
  private final boolean services;
  private final String routerPrefix;
  private final String routerSuffix;
  private final Consumer<String> output;
  private final List<MvcRouter> routers = new ArrayList<>();
  private final Map<TypeElement, Map.Entry<String, String>> handler = new HashMap<>();

  public MvcContext(ProcessingEnvironment processingEnvironment, Consumer<String> output) {
    this.processingEnvironment = processingEnvironment;
    this.output = output;
    this.debug = Options.boolOpt(processingEnvironment, Options.DEBUG, false);
    this.incremental = Options.boolOpt(processingEnvironment, Options.INCREMENTAL, true);
    this.services = Options.boolOpt(processingEnvironment, Options.SERVICES, true);
    this.routerPrefix = Options.string(processingEnvironment, Options.ROUTER_PREFIX, "");
    this.routerSuffix = Options.string(processingEnvironment, Options.ROUTER_SUFFIX, "_");
    computeResultTypes(processingEnvironment, handler::put);

    debug("Incremental annotation processing is turned %s.", incremental ? "ON" : "OFF");
    debug("Generation of service provider configuration is turned %s.", services ? "ON" : "OFF");
  }

  private void computeResultTypes(
      ProcessingEnvironment processingEnvironment,
      BiConsumer<TypeElement, Map.Entry<String, String>> consumer) {
    var handler =
        new HashSet<>(
            Set.of(
                "io.jooby.ReactiveSupport",
                "io.jooby.mutiny.Mutiny",
                "io.jooby.reactor.Reactor",
                "io.jooby.rxjava3.Reactivex"));
    handler.addAll(Options.stringListOpt(processingEnvironment, Options.HANDLER));
    handler.stream()
        .map(type -> processingEnvironment.getElementUtils().getTypeElement(type))
        .filter(Objects::nonNull)
        .forEach(
            it -> {
              var annotation =
                  AnnotationSupport.findAnnotationByName(it, "io.jooby.annotation.ResultType");
              if (annotation != null) {
                var handlerFunction =
                    AnnotationSupport.findAnnotationValue(annotation, "handler"::equals).get(0);
                Map.Entry<String, String> entry;
                var i = handlerFunction.lastIndexOf('.');
                if (i > 0) {
                  var container = handlerFunction.substring(0, i);
                  var fn = handlerFunction.substring(i + 1);
                  entry = Map.entry(container, fn);
                } else {
                  entry = Map.entry(it.asType().toString(), handlerFunction);
                }
                var functions =
                    it.getEnclosedElements().stream()
                        .filter(ExecutableElement.class::isInstance)
                        .map(ExecutableElement.class::cast)
                        .filter(m -> entry.getValue().equals(m.getSimpleName().toString()))
                        .toList();
                if (functions.isEmpty()) {
                  throw new IllegalArgumentException(
                      "Method not found: " + entry.getKey() + "." + entry.getValue());
                } else {
                  var args =
                      functions.stream()
                          .filter(
                              m ->
                                  !m.getParameters().isEmpty()
                                      && m.getParameters()
                                          .get(0)
                                          .asType()
                                          .toString()
                                          .equals("io.jooby.Route.Handler"))
                          .findFirst()
                          .orElseThrow(
                              () ->
                                  new IllegalArgumentException(
                                      "Signature doesn't match: "
                                          + functions
                                          + " must be: "
                                          + functions.stream()
                                              .map(
                                                  e ->
                                                      e.getSimpleName()
                                                          + "(io.jooby.Route.Handler)")
                                              .collect(Collectors.joining(", ", "[", "]"))));
                  if (!args.getReturnType().toString().equals("io.jooby.Route.Handler")) {
                    throw new IllegalArgumentException(
                        "Method returns type not supported: "
                            + args
                            + ": "
                            + args.getReturnType()
                            + " must be: "
                            + args
                            + ": io.jooby.Route.Handler");
                  }
                  if (!args.getModifiers().contains(Modifier.STATIC)) {
                    throw new IllegalArgumentException("Method must be static: " + args);
                  }
                }
                var types =
                    AnnotationSupport.findAnnotationValue(
                        annotation, "types"::equals, value -> (DeclaredType) value.getValue());
                for (var type : types) {
                  superTypes(type.asElement()).forEach(t -> consumer.accept(t, entry));
                }
              }
            });
  }

  public void add(MvcRouter router) {
    routers.add(router);
  }

  public List<MvcRouter> getRouters() {
    return routers;
  }

  public String generateRouterName(String name) {
    var i = name.lastIndexOf('.');
    if (i > 0) {
      return name.substring(0, i + 1) + routerPrefix + name.substring(i + 1) + routerSuffix;
    }
    return routerPrefix + name + routerSuffix;
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

  /**
   * Find path from route method and router type. This method scan and expand path base on the
   * annotation present at method or class level.
   *
   * @param owner Router type.
   * @param exec Method.
   * @param annotation HTTP annotation. One of {@link HttpMethod}.
   * @return List of possible paths.
   */
  public List<String> path(TypeElement owner, ExecutableElement exec, TypeElement annotation) {
    var prefix = HttpPath.PATH.path(superTypes(owner));
    // Favor GET("/path") over Path("/path") at method level
    var httpMethod = HttpMethod.findByAnnotationName(annotation.getQualifiedName().toString());
    var path = httpMethod.path(annotation);
    if (path.isEmpty()) {
      path = httpMethod.path(exec);
    }
    if (prefix.isEmpty()) {
      return path.isEmpty() ? Collections.singletonList("/") : path;
    }
    if (path.isEmpty()) {
      return prefix;
    }
    var methodPath = path;
    return prefix.stream()
        .flatMap(root -> methodPath.stream().map(p -> root.equals("/") ? p : root + p))
        .distinct()
        .toList();
  }

  public String pipeline(TypeMirror returnType, String handlerReference) {
    var entry = findMappingHandler(returnType);
    return entry == null ? handlerReference : entry.getValue() + "(" + handlerReference + ")";
  }

  private Map.Entry<String, String> findMappingHandler(TypeMirror type) {
    for (var e : handler.entrySet()) {
      if (type.toString().equals(e.getKey().toString())
          || processingEnvironment.getTypeUtils().isAssignable(type, e.getKey().asType())) {
        return e.getValue();
      }
    }
    return null;
  }

  /**
   * Find all super types for given type. Produces a set of super types in keeping the type
   * hierarchy. This method includes the input type as first element.
   *
   * @param owner Input type.
   * @return Type hierarchy.
   */
  public Set<TypeElement> superTypes(Element owner) {
    var typeUtils = processingEnvironment.getTypeUtils();
    var supertypes = typeUtils.directSupertypes(owner.asType());
    Set<TypeElement> result = new LinkedHashSet<>();
    result.add((TypeElement) owner);
    if (supertypes == null || supertypes.isEmpty()) {
      return result;
    }
    var supertype = supertypes.get(0);
    var supertypeName = typeUtils.erasure(supertype).toString();
    var supertypeElement = typeUtils.asElement(supertype);
    if (!Object.class.getName().equals(supertypeName)
        && supertypeElement.getKind() == ElementKind.CLASS) {
      result.addAll(superTypes(supertypeElement));
    }
    return result;
  }

  public boolean generateServices() {
    return services;
  }

  public boolean isIncremental() {
    return incremental;
  }

  public boolean isServices() {
    return services;
  }

  public void debug(String message, Object... args) {
    if (debug) {
      info(message, args);
    }
  }

  public void info(String message, Object... args) {
    var msg = args.length == 0 ? message : message.formatted(args);
    output.accept(msg);
  }

  public void generateStaticImports(MvcRouter mvcRouter, BiConsumer<String, String> consumer) {
    List<MvcRoute> routes = mvcRouter.getRoutes();
    var process = new HashSet<String>();
    for (MvcRoute route : routes) {
      var returnType = route.getReturnTypeHandler();
      if (process.add(returnType.toString())) {
        var fnq = findMappingHandler(returnType);
        if (fnq != null) {
          consumer.accept(fnq.getKey(), fnq.getValue());
        }
      }
    }
  }
}
