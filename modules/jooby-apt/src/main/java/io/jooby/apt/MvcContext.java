/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import java.util.*;
import java.util.function.Consumer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

import io.jooby.apt.JoobyProcessor.Options;
import io.jooby.internal.apt.HttpMethod;
import io.jooby.internal.apt.HttpPath;
import io.jooby.internal.apt.MvcRouter;

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

  public MvcContext(ProcessingEnvironment processingEnvironment, Consumer<String> output) {
    this.processingEnvironment = processingEnvironment;
    this.output = output;
    this.debug = Options.boolOpt(processingEnvironment, Options.DEBUG, false);
    this.incremental = Options.boolOpt(processingEnvironment, Options.INCREMENTAL, true);
    this.services = Options.boolOpt(processingEnvironment, Options.SERVICES, true);
    this.routerPrefix = Options.string(processingEnvironment, Options.ROUTER_PREFIX, "");
    this.routerSuffix = Options.string(processingEnvironment, Options.ROUTER_SUFFIX, "_");

    debug("Incremental annotation processing is turned %s.", incremental ? "ON" : "OFF");
    debug("Generation of service provider configuration is turned %s.", services ? "ON" : "OFF");
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
}
