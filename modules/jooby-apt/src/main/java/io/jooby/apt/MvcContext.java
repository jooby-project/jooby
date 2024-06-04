/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import java.util.*;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

import io.jooby.apt.JoobyProcessor.Options;
import io.jooby.internal.apt.HttpMethod;
import io.jooby.internal.apt.HttpPath;
import io.jooby.internal.apt.MvcRouter;

public class MvcContext {
  private final ProcessingEnvironment processingEnvironment;
  private final boolean debug;
  private final boolean incremental;
  private final boolean services;
  private int round;
  private final Messager messager;
  private final List<MvcRouter> routers = new ArrayList<>();

  public MvcContext(ProcessingEnvironment processingEnvironment, Messager messager) {
    this.processingEnvironment = processingEnvironment;
    this.messager = messager;
    this.debug = Options.boolOpt(processingEnvironment, Options.OPT_DEBUG, false);
    this.incremental = Options.boolOpt(processingEnvironment, Options.OPT_INCREMENTAL, true);
    this.services = Options.boolOpt(processingEnvironment, Options.OPT_SERVICES, true);

    debug("Incremental annotation processing is turned %s.", incremental ? "ON" : "OFF");
    debug("Generation of service provider configuration is turned %s.", services ? "ON" : "OFF");
  }

  public void add(MvcRouter router) {
    routers.add(router);
  }

  public List<MvcRouter> getRouters() {
    return routers;
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

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

  public List<TypeElement> superTypes(Element owner) {
    var typeUtils = processingEnvironment.getTypeUtils();
    var supertypes = typeUtils.directSupertypes(owner.asType());
    List<TypeElement> result = new ArrayList<>();
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

  public int nextRound() {
    return ++round;
  }

  public void debug(String message, Object... args) {
    if (debug) {
      print(Diagnostic.Kind.NOTE, message, args);
    }
  }

  public void error(String message, Object... args) {
    print(Diagnostic.Kind.ERROR, message, args);
  }

  public void warning(String message, Object... args) {
    print(Diagnostic.Kind.WARNING, message, args);
  }

  public void info(String message, Object... args) {
    print(Diagnostic.Kind.NOTE, message, args);
  }

  private void print(Diagnostic.Kind kind, String message, Object... args) {
    var msg = message.formatted(args);
    Element element =
        args.length > 0
            ? (args[args.length - 1] instanceof Element) ? (Element) args[args.length - 1] : null
            : null;
    messager.printMessage(kind, msg, element);
  }
}
