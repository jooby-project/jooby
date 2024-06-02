/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;

import io.jooby.internal.apt.Annotations;
import io.jooby.internal.apt.Opts;
import io.jooby.internal.newapt.MvcRouter;

@SupportedOptions({
  Opts.OPT_DEBUG,
  Opts.OPT_INCREMENTAL,
  Opts.OPT_SERVICES,
  Opts.OPT_SKIP_ATTRIBUTE_ANNOTATIONS,
  Opts.OPT_EXTENDED_LOOKUP_OF_SUPERTYPES
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MvcSourceCodeProcessor extends AbstractProcessor {
  private MvcContext context;
  private Messager messager;
  private final Set<Object> processed = new HashSet<>();

  public MvcSourceCodeProcessor(Messager messager) {
    this.messager = messager;
  }

  public MvcSourceCodeProcessor() {}

  @Override
  public synchronized void init(ProcessingEnvironment processingEnvironment) {
    this.context =
        new MvcContext(
            processingEnvironment,
            ofNullable(messager).orElse(processingEnvironment.getMessager()));
    super.init(processingEnvironment);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    context.debug("round #%s", context.nextRound());

    if (roundEnv.processingOver()) {
      if (context.generateServices()) {
        // TODO: doServices(processingEnv.getFiler(), modules);
      }
      return false;
    } else {
      var routeMap = buildRouteRegistry(annotations, roundEnv);
      var filer = context.getProcessingEnvironment().getFiler();
      for (var router : routeMap.values()) {
        try {
          var javaFile = router.toSourceCode();
          context.debug("%s", javaFile);
          javaFile.writeTo(filer);
        } catch (IOException cause) {
          throw new RuntimeException("Unable to generate: " + router.getTargetType(), cause);
        }
      }
      return true;
    }
  }

  private Map<TypeElement, MvcRouter> buildRouteRegistry(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Map<TypeElement, MvcRouter> registry = new LinkedHashMap<>();

    for (var annotation : annotations) {
      var elements = roundEnv.getElementsAnnotatedWith(annotation);
      // Element could be Class or Method, bc @Path can be applied to both of them
      // Also we need to expand lookup to external jars see #2486
      for (var element : elements) {
        if (element instanceof TypeElement typeElement) {
          buildRouteRegistry(registry, typeElement);
        } else if (element instanceof ExecutableElement method) {
          buildRouteRegistry(registry, (TypeElement) method.getEnclosingElement());
        }
      }
    }
    // Remove all abstract router
    var abstractTypes =
        registry.entrySet().stream()
            .filter(it -> it.getValue().isAbstract())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    abstractTypes.forEach(registry::remove);

    // Generate unique method name by router
    for (var router : registry.values()) {
      var names = new HashSet<String>();
      for (var route : router.getRoutes()) {
        if (!names.add(route.getMethodName())) {
          var paramsString =
              route.getRawParameterTypes().stream()
                  .map(it -> it.substring(Math.max(0, it.lastIndexOf(".") + 1)))
                  .map(it -> Character.toUpperCase(it.charAt(0)) + it.substring(1))
                  .collect(Collectors.joining());
          route.setGeneratedName(route.getMethodName() + paramsString);
        } else {
          route.setGeneratedName(route.getMethodName());
        }
      }
    }
    return registry;
  }

  /**
   * Scan routes from basType and any super class of it. It saves all route method found in current
   * type or super (inherited). Routes method from super types are also saved.
   *
   * <p>Abstract route method are ignored.
   *
   * @param registry Route registry.
   * @param currentType Base type.
   */
  private void buildRouteRegistry(Map<TypeElement, MvcRouter> registry, TypeElement currentType) {
    for (TypeElement superType : context.superTypes(currentType)) {
      if (processed.add(superType)) {
        // collect all declared methods
        superType.getEnclosedElements().stream()
            .filter(ExecutableElement.class::isInstance)
            .map(ExecutableElement.class::cast)
            .forEach(
                method -> {
                  if (method.getModifiers().contains(Modifier.ABSTRACT)) {
                    context.debug("ignoring abstract method %s", superType, method);
                  } else {
                    method.getAnnotationMirrors().stream()
                        .map(AnnotationMirror::getAnnotationType)
                        .map(DeclaredType::asElement)
                        .filter(TypeElement.class::isInstance)
                        .map(TypeElement.class::cast)
                        .filter(annotation -> context.isHttpMethod(annotation))
                        .forEach(
                            annotation -> {
                              Stream.of(currentType, superType)
                                  .distinct()
                                  .forEach(
                                      routerClass ->
                                          registry
                                              .computeIfAbsent(
                                                  routerClass, type -> new MvcRouter(context, type))
                                              .put(annotation, method));
                            });
                  }
                });
      } else {
        context.debug("already processed", superType);
      }
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Stream.concat(Annotations.PATH.stream(), Annotations.HTTP_METHODS.stream())
        .collect(Collectors.toSet());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public Set<String> getSupportedOptions() {
    var options = new HashSet<>(super.getSupportedOptions());

    if (context.isIncremental()) {
      // Enables incremental annotation processing support in Gradle.
      // If service provider configuration is being generated,
      // only 'aggregating' mode is supported since it's likely that
      // more then one originating element is passed to the Filer
      // API on writing the resource file - isolating mode does not
      // allow this.
      options.add(
          String.format(
              "org.gradle.annotation.processing.%s",
              context.isServices() ? "aggregating" : "isolating"));
    }

    return options;
  }
}
