/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import io.jooby.internal.apt.Annotations;
import io.jooby.internal.apt.HandlerCompiler;
import io.jooby.internal.apt.JoobyTypes;
import io.jooby.internal.apt.ModuleCompiler;
import io.jooby.internal.apt.Opts;

/**
 * Jooby Annotation Processing Tool. It generates byte code for MVC routes.
 *
 * @since 2.1.0
 */
@SupportedOptions({
  Opts.OPT_DEBUG,
  Opts.OPT_INCREMENTAL,
  Opts.OPT_SERVICES,
  Opts.OPT_SKIP_ATTRIBUTE_ANNOTATIONS,
  Opts.OPT_EXTENDED_LOOKUP_OF_SUPERTYPES
})
public class JoobyProcessor extends AbstractProcessor {

  private ProcessingEnvironment processingEnv;

  private boolean debug;
  private boolean incremental;
  private boolean services;
  private boolean extendedLookupOfSuperTypes;

  private int round;
  private Map<TypeElement, String> modules = new LinkedHashMap<>();
  private Set<String> alreadyProcessed = new HashSet<>();

  @Override
  public Set<String> getSupportedOptions() {
    Set<String> options = new HashSet<>(super.getSupportedOptions());

    if (incremental) {
      // Enables incremental annotation processing support in Gradle.
      // If service provider configuration is being generated,
      // only 'aggregating' mode is supported since it's likely that
      // more then one originating element is passed to the Filer
      // API on writing the resource file - isolating mode does not
      // allow this.
      options.add(
          String.format(
              "org.gradle.annotation.processing.%s", services ? "aggregating" : "isolating"));
    }

    return options;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Stream.concat(Annotations.PATH.stream(), Annotations.HTTP_METHODS.stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnvironment) {
    this.processingEnv = processingEnvironment;

    debug = Opts.boolOpt(processingEnv, Opts.OPT_DEBUG, false);
    incremental = Opts.boolOpt(processingEnv, Opts.OPT_INCREMENTAL, true);
    services = Opts.boolOpt(processingEnv, Opts.OPT_SERVICES, true);
    extendedLookupOfSuperTypes =
        Opts.boolOpt(processingEnv, Opts.OPT_EXTENDED_LOOKUP_OF_SUPERTYPES, true);

    debug("Incremental annotation processing is turned %s.", incremental ? "ON" : "OFF");
    debug("Generation of service provider configuration is turned %s.", services ? "ON" : "OFF");
    debug("Extended lookup of superTypes %s.", extendedLookupOfSuperTypes ? "ON" : "OFF");
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      debug("Round #%s", round++);
      if (roundEnv.processingOver()) {
        if (services) {
          doServices(processingEnv.getFiler(), modules);
        }
        return false;
      } else {
        Map<TypeElement, Map<TypeElement, List<ExecutableElement>>> routeMap =
            collectRoutes(annotations, roundEnv);

        Map<TypeElement, String> modules =
            build(processingEnv.getFiler(), classes(routeMap), alreadyProcessed::add);
        alreadyProcessed.addAll(modules.values());
        this.modules.putAll(modules);

        return true;
      }
    } catch (Exception x) {
      throw propagate(x);
    }
  }

  private Map<TypeElement, Map<TypeElement, List<ExecutableElement>>> collectRoutes(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Map<TypeElement, Map<TypeElement, List<ExecutableElement>>> routeMap = new LinkedHashMap<>();

    for (TypeElement annotation : annotations) {
      Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);

      /**
       * Add empty-subclass (edge case where you mark something with @Path and didn't add any HTTP
       * annotation.
       */
      elements.stream()
          .filter(TypeElement.class::isInstance)
          .map(TypeElement.class::cast)
          .filter(type -> !type.getModifiers().contains(Modifier.ABSTRACT))
          .forEach(e -> routeMap.computeIfAbsent(e, k -> new LinkedHashMap<>()));

      if (Annotations.HTTP_METHODS.contains(annotation.asType().toString())) {
        Set<ExecutableElement> methods =
            elements.stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (ExecutableElement method : methods) {
          Map<TypeElement, List<ExecutableElement>> mapping =
              routeMap.computeIfAbsent(
                  (TypeElement) method.getEnclosingElement(), k -> new LinkedHashMap<>());
          mapping.computeIfAbsent(annotation, k -> new ArrayList<>()).add(method);
        }
      } else {
        if (extendedLookupOfSuperTypes) {
          elements.stream()
              .filter(TypeElement.class::isInstance)
              .map(TypeElement.class::cast)
              .forEach(
                  parentTypeElement -> extendedLookupOfSuperTypes(routeMap, parentTypeElement));
        }
      }
    }
    return routeMap;
  }

  /**
   * Crawls through the sub-classes. Inspects them for HTTP Method annotated entries
   *
   * @param routeMap
   * @param parentTypeElement
   */
  private void extendedLookupOfSuperTypes(
      Map<TypeElement, Map<TypeElement, List<ExecutableElement>>> routeMap,
      TypeElement parentTypeElement) {
    for (TypeElement superType : superTypes(parentTypeElement)) {
      // collect all declared methods
      Set<ExecutableElement> methods =
          superType.getEnclosedElements().stream()
              .filter(ExecutableElement.class::isInstance)
              .map(ExecutableElement.class::cast)
              .collect(Collectors.toCollection(LinkedHashSet::new));
      for (ExecutableElement method : methods) {
        // extract all annotation type elements
        LinkedHashSet<TypeElement> annotationTypes =
            method.getAnnotationMirrors().stream()
                .map(AnnotationMirror::getAnnotationType)
                .map(DeclaredType::asElement)
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (TypeElement annotationType : annotationTypes) {
          if (Annotations.HTTP_METHODS.contains(annotationType.toString())) {
            // ensure map is created for parent type element
            Map<TypeElement, List<ExecutableElement>> mapping =
                routeMap.computeIfAbsent(parentTypeElement, k -> new LinkedHashMap<>());
            List<ExecutableElement> list =
                mapping.computeIfAbsent(annotationType, k -> new ArrayList<>());
            // ensure that the same method wasnt already defined in parent
            if (list.stream().map(this::signature).noneMatch(signature(method)::equals)) {
              list.add(method);
            }
          }
        }
      }
    }
  }

  private Map<TypeElement, String> build(
      Filer filer, Map<TypeElement, List<HandlerCompiler>> classes, Predicate<String> includes)
      throws Exception {
    Types typeUtils = processingEnv.getTypeUtils();

    Map<TypeElement, String> modules = new LinkedHashMap<>();
    for (Map.Entry<TypeElement, List<HandlerCompiler>> entry : classes.entrySet()) {
      TypeElement type = entry.getKey();
      String typeName = typeUtils.erasure(type.asType()).toString();
      if (includes.test(typeName)) {
        List<HandlerCompiler> handlers = entry.getValue();
        ModuleCompiler module = new ModuleCompiler(processingEnv, typeName);
        String moduleClass = module.getModuleClass();
        byte[] moduleBin = module.compile(handlers);
        onClass(moduleClass, moduleBin);
        writeClass(filer.createClassFile(moduleClass, type), moduleBin);
        modules.put(type, moduleClass);
      }
    }

    return modules;
  }

  private Map<TypeElement, List<HandlerCompiler>> classes(
      Map<TypeElement, Map<TypeElement, List<ExecutableElement>>> routeMap) {
    Map<TypeElement, List<HandlerCompiler>> classes = new LinkedHashMap<>();
    for (Map.Entry<TypeElement, Map<TypeElement, List<ExecutableElement>>> e :
        routeMap.entrySet()) {
      TypeElement type = e.getKey();
      boolean isAbstract = type.getModifiers().contains(Modifier.ABSTRACT);
      /** Ignore abstract routes: */
      if (!isAbstract) {
        /** Expand route method from superclass(es): */
        Map<TypeElement, List<ExecutableElement>> mappings = e.getValue();
        for (TypeElement superType : superTypes(type)) {
          Map<TypeElement, List<ExecutableElement>> baseMappings =
              routeMap.getOrDefault(superType, Collections.emptyMap());
          for (Map.Entry<TypeElement, List<ExecutableElement>> be : baseMappings.entrySet()) {
            List<ExecutableElement> methods = mappings.get(be.getKey());
            if (methods == null) {
              mappings.put(be.getKey(), be.getValue());
            } else {
              for (ExecutableElement it : be.getValue()) {
                String signature = signature(it);
                if (methods.stream().map(this::signature).noneMatch(signature::equals)) {
                  methods.add(it);
                }
              }
            }
          }
        }
        /** Route method ready, creates a Route.Handler for each of them: */
        for (Map.Entry<TypeElement, List<ExecutableElement>> mapping : mappings.entrySet()) {
          TypeElement httpMethod = mapping.getKey();
          List<ExecutableElement> methods = mapping.getValue();
          for (ExecutableElement method : methods) {
            debug("Found method %s.%s", type, method);
            List<String> paths = path(type, httpMethod, method);
            for (String path : paths) {
              debug("  route %s %s", httpMethod.getSimpleName(), path);
              HandlerCompiler compiler =
                  new HandlerCompiler(processingEnv, type, method, httpMethod, path);
              classes.computeIfAbsent(type, k -> new ArrayList<>()).add(compiler);
            }
          }
        }
      }
    }
    return classes;
  }

  private String signature(ExecutableElement method) {
    return method.toString();
  }

  private List<TypeElement> superTypes(Element owner) {
    Types typeUtils = processingEnv.getTypeUtils();
    List<? extends TypeMirror> supertypes = typeUtils.directSupertypes(owner.asType());
    if (supertypes == null || supertypes.isEmpty()) {
      return Collections.emptyList();
    }
    TypeMirror supertype = supertypes.get(0);
    String supertypeName = typeUtils.erasure(supertype).toString();
    Element supertypeElement = typeUtils.asElement(supertype);
    if (!Object.class.getName().equals(supertypeName)
        && supertypeElement.getKind() == ElementKind.CLASS) {
      List<TypeElement> result = new ArrayList<>();
      result.addAll(superTypes(supertypeElement));
      result.add((TypeElement) supertypeElement);
      return result;
    }
    return Collections.emptyList();
  }

  private void debug(String format, Object... args) {
    if (debug) {
      System.out.printf(format + "\n", args);
    }
  }

  private void doServices(Filer filer, Map<TypeElement, String> modules) throws IOException {
    String location = "META-INF/services/" + JoobyTypes.MvcFactory.getClassName();
    debug("%s", location);

    Element[] originatingElements = modules.keySet().toArray(new Element[0]);
    FileObject resource =
        filer.createResource(StandardLocation.CLASS_OUTPUT, "", location, originatingElements);
    StringBuilder content = new StringBuilder();
    for (Map.Entry<TypeElement, String> e : modules.entrySet()) {
      String classname = e.getValue();
      debug("  %s", classname);
      content.append(classname).append(System.getProperty("line.separator"));
    }
    onResource(location, content.toString());
    try (PrintWriter writer = new PrintWriter(resource.openOutputStream())) {
      writer.println(content);
    }
  }

  protected void onClass(String className, byte[] bytecode) {}

  protected void onResource(String location, String content) {}

  private void writeClass(JavaFileObject javaFileObject, byte[] bytecode) throws IOException {
    try (OutputStream output = javaFileObject.openOutputStream()) {
      output.write(bytecode);
    }
  }

  private List<String> path(Element owner, TypeElement annotation, ExecutableElement exec) {
    List<String> prefix = path(owner);
    if (prefix.isEmpty()) {
      // Look at parent @path annotation
      List<TypeElement> superTypes = superTypes(owner);
      int i = superTypes.size() - 1;
      while (prefix.isEmpty() && i >= 0) {
        prefix = path(superTypes.get(i--));
      }
    }

    // Favor GET("/path") over Path("/path") at method level
    List<String> path =
        path(annotation.getQualifiedName().toString(), annotation.getAnnotationMirrors());
    if (path.isEmpty()) {
      path = path(annotation.getQualifiedName().toString(), exec.getAnnotationMirrors());
    }
    List<String> methodPath = path;
    if (prefix.isEmpty()) {
      return path.isEmpty() ? Collections.singletonList("/") : path;
    }
    if (path.isEmpty()) {
      return prefix.isEmpty() ? Collections.singletonList("/") : prefix;
    }
    return prefix.stream()
        .flatMap(root -> methodPath.stream().map(p -> root.equals("/") ? p : root + p))
        .distinct()
        .collect(Collectors.toList());
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
        .collect(Collectors.toList());
  }

  /**
   * Throws any throwable 'sneakily' - you don't need to catch it, nor declare that you throw it
   * onwards. The exception is still thrown - javac will just stop whining about it.
   *
   * <p>Example usage:
   *
   * <pre>public void run() {
   *     throw sneakyThrow(new IOException("You don't need to catch me!"));
   * }</pre>
   *
   * <p>NB: The exception is not wrapped, ignored, swallowed, or redefined. The JVM actually does
   * not know or care about the concept of a 'checked exception'. All this method does is hide the
   * act of throwing a checked exception from the java compiler.
   *
   * <p>Note that this method has a return type of {@code RuntimeException}; it is advised you
   * always call this method as argument to the {@code throw} statement to avoid compiler errors
   * regarding no return statement and similar problems. This method won't of course return an
   * actual {@code RuntimeException} - it never returns, it always throws the provided exception.
   *
   * @param x The throwable to throw without requiring you to catch its type.
   * @return A dummy RuntimeException; this method never returns normally, it <em>always</em> throws
   *     an exception!
   */
  public static RuntimeException propagate(final Throwable x) {
    if (x == null) {
      throw new NullPointerException("x");
    }

    sneakyThrow0(x);
    return null;
  }

  /**
   * Make a checked exception un-checked and rethrow it.
   *
   * @param x Exception to throw.
   * @param <E> Exception type.
   * @throws E Exception to throw.
   */
  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow0(final Throwable x) throws E {
    throw (E) x;
  }
}
