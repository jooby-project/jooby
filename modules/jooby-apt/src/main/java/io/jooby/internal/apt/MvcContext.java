/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.function.BiConsumer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import io.jooby.apt.JoobyProcessor.Options;

/**
 * Utility class which give access to {@link ProcessingEnvironment} mainly, few utility methods and
 * processor options.
 */
public class MvcContext {
  private final ProcessingEnvironment processingEnvironment;
  private final boolean debug;
  private final boolean incremental;
  private final String routerPrefix;
  private final String routerSuffix;
  private final BiConsumer<Diagnostic.Kind, String> output;
  private final List<MvcRouter> routers = new ArrayList<>();
  private final boolean mvcMethod;
  private final Map<TypeElement, ReactiveType> reactiveTypeMap = new HashMap<>();

  public MvcContext(
      ProcessingEnvironment processingEnvironment, BiConsumer<Diagnostic.Kind, String> output) {
    this.processingEnvironment = processingEnvironment;
    this.output = output;
    this.debug = Options.boolOpt(processingEnvironment, Options.DEBUG, false);
    this.incremental = Options.boolOpt(processingEnvironment, Options.INCREMENTAL, true);
    this.mvcMethod = Options.boolOpt(processingEnvironment, Options.MVC_METHOD, false);
    this.routerPrefix = Options.string(processingEnvironment, Options.ROUTER_PREFIX, "");
    this.routerSuffix = Options.string(processingEnvironment, Options.ROUTER_SUFFIX, "_");
    computeReactiveTypes(processingEnvironment, reactiveTypeMap::put);

    debug("Incremental annotation processing is turned %s.", incremental ? "ON" : "OFF");
  }

  private void computeReactiveTypes(
      ProcessingEnvironment processingEnvironment, BiConsumer<TypeElement, ReactiveType> consumer) {
    ReactiveType.supportedTypes()
        .forEach(
            reactiveType -> {
              var handlerType =
                  processingEnvironment
                      .getElementUtils()
                      .getTypeElement(reactiveType.handlerType());
              if (handlerType != null) {
                // Handler Type is on classpath
                reactiveType.reactiveTypes().stream()
                    .map(it -> processingEnvironment.getElementUtils().getTypeElement(it))
                    .forEach(
                        it -> {
                          superTypes(it).forEach(t -> consumer.accept(t, reactiveType));
                        });
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
    return entry == null ? handlerReference : entry.handler() + "(" + handlerReference + ")";
  }

  public boolean nonBlocking(TypeMirror returnType) {
    var entry = findMappingHandler(returnType);
    return entry != null;
  }

  private ReactiveType findMappingHandler(TypeMirror type) {
    for (var e : reactiveTypeMap.entrySet()) {
      var that = e.getKey();
      if (type.toString().equals(that.toString())
          || processingEnvironment.getTypeUtils().isAssignable(type, that.asType())) {
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

  public boolean generateMvcMethod() {
    return mvcMethod;
  }

  public boolean isIncremental() {
    return incremental;
  }

  public void debug(String message, Object... args) {
    if (debug) {
      report(Diagnostic.Kind.OTHER, message, args);
    }
  }

  public void error(String message, Object... args) {
    Throwable cause =
        args.length > 0 && args[args.length - 1] instanceof Throwable
            ? (Throwable) args[args.length - 1]
            : null;
    if (cause != null) {
      var str = new StringWriter();
      cause.printStackTrace(new PrintWriter(str, true));
      var errorMessage = cause.getMessage();
      args[args.length - 1] =
          (errorMessage == null || errorMessage.equals(message) ? "" : errorMessage) + ":\n" + str;
      message += ": %s";
    }
    report(Diagnostic.Kind.ERROR, message, args);
  }

  private void report(Diagnostic.Kind kind, String message, Object... args) {
    var msg = args.length == 0 ? message : message.formatted(args);
    output.accept(kind, msg);
  }

  public void generateStaticImports(MvcRouter mvcRouter, BiConsumer<String, String> consumer) {
    List<MvcRoute> routes = mvcRouter.getRoutes();
    var process = new HashSet<String>();
    for (MvcRoute route : routes) {
      var returnType = route.getReturnTypeHandler();
      if (process.add(returnType.toString())) {
        var fnq = findMappingHandler(returnType);
        if (fnq != null) {
          consumer.accept(fnq.handlerType(), fnq.handler());
        }
      }
    }
  }
}
