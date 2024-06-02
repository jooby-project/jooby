/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import java.util.*;
import java.util.stream.Stream;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

import io.jooby.internal.apt.Annotations;
import io.jooby.internal.apt.Opts;

public class MvcContext {
  private final ProcessingEnvironment processingEnvironment;
  private final boolean debug;
  private final boolean incremental;
  private final boolean services;
  private int round;
  private final Messager messager;
  private final Map<Object, Object> attributes = new HashMap<>();

  public MvcContext(ProcessingEnvironment processingEnvironment, Messager messager) {
    this.processingEnvironment = processingEnvironment;
    this.messager = messager;
    this.debug = Opts.boolOpt(processingEnvironment, Opts.OPT_DEBUG, false);
    this.incremental = Opts.boolOpt(processingEnvironment, Opts.OPT_INCREMENTAL, true);
    this.services = Opts.boolOpt(processingEnvironment, Opts.OPT_SERVICES, true);

    debug("Incremental annotation processing is turned %s.", incremental ? "ON" : "OFF");
    debug("Generation of service provider configuration is turned %s.", services ? "ON" : "OFF");
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

  public Map<Object, Object> getAttributes() {
    return attributes;
  }

  public boolean isHttpMethod(TypeElement annotated) {
    if (annotated == null) {
      return false;
    }
    return Annotations.HTTP_METHODS.contains(annotated.toString())
        || Annotations.HTTP_METHODS.contains(annotated.asType().toString());
  }

  public List<String> path(TypeElement owner, ExecutableElement exec, TypeElement annotation) {
    var prefix = Collections.<String>emptyList();
    // Look at parent @path annotation
    var superTypes = superTypes(owner);
    var i = 0;
    while (prefix.isEmpty() && i < superTypes.size()) {
      prefix = path(superTypes.get(i++));
    }

    // Favor GET("/path") over Path("/path") at method level
    var path = path(annotation.getQualifiedName().toString(), annotation.getAnnotationMirrors());
    if (path.isEmpty()) {
      path = path(annotation.getQualifiedName().toString(), exec.getAnnotationMirrors());
    }
    var methodPath = path;
    if (prefix.isEmpty()) {
      return path.isEmpty() ? Collections.singletonList("/") : path;
    }
    if (path.isEmpty()) {
      return prefix;
    }
    return prefix.stream()
        .flatMap(root -> methodPath.stream().map(p -> root.equals("/") ? p : root + p))
        .distinct()
        .toList();
  }

  private List<String> path(Element element) {
    return path(null, element.getAnnotationMirrors());
  }

  private List<String> path(String method, List<? extends AnnotationMirror> annotations) {
    return annotations.stream()
        .map(AnnotationMirror.class::cast)
        .flatMap(
            mirror -> {
              String type = mirror.getAnnotationType().toString();
              if (Annotations.PATH.contains(type) || type.equals(method)) {
                return Stream.concat(
                    Annotations.attribute(mirror, "path").stream(),
                    Annotations.attribute(mirror, "value").stream());
              }
              return Stream.empty();
            })
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
