/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import io.jooby.MvcFactory;
import io.jooby.SneakyThrows;
import io.jooby.internal.apt.HandlerCompiler;
import io.jooby.internal.apt.ModuleCompiler;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Jooby Annotation Processing Tool. It generates byte code for MVC routes.
 *
 * @since 2.1.0
 */
public class JoobyProcessor extends AbstractProcessor {

  private ProcessingEnvironment processingEnv;

  /**
   * Route Data.
   * {
   *   HTTP_METHOD: [method1, ..., methodN]
   * }
   */
  private Map<TypeElement, Map<TypeElement, List<ExecutableElement>>> routeMap = new LinkedHashMap<>();

  private boolean debug;

  private int round;

  @Override public Set<String> getSupportedAnnotationTypes() {
    return Stream.concat(Annotations.PATH.stream(), Annotations.HTTP_METHODS.stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public void init(ProcessingEnvironment processingEnvironment) {
    this.processingEnv = processingEnvironment;
    debug = Boolean.parseBoolean(processingEnvironment.getOptions().getOrDefault("debug", "false"));
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      debug("Round #%s", round++);
      if (roundEnv.processingOver()) {

        build(processingEnv.getFiler());

        return false;
      }

      /**
       * Do MVC handler: per each mvc method we create a Route.Handler.
       */
      for (TypeElement annotation : annotations) {
        Set<? extends Element> elements = roundEnv
            .getElementsAnnotatedWith(annotation);

        /**
         * Add empty-subclass (edge case where you mark something with @Path and didn't add any
         * HTTP annotation.
         */
        elements.stream()
            .filter(TypeElement.class::isInstance)
            .map(TypeElement.class::cast)
            .filter(type -> !type.getModifiers().contains(Modifier.ABSTRACT))
            .forEach(e -> routeMap.computeIfAbsent(e, k -> new LinkedHashMap<>()));

        if (Annotations.HTTP_METHODS.contains(annotation.asType().toString())) {
          Set<ExecutableElement> methods = elements.stream()
              .filter(ExecutableElement.class::isInstance)
              .map(ExecutableElement.class::cast)
              .collect(Collectors.toCollection(LinkedHashSet::new));
          for (ExecutableElement method : methods) {
            Map<TypeElement, List<ExecutableElement>> mapping = routeMap
                .computeIfAbsent((TypeElement) method.getEnclosingElement(),
                    k -> new LinkedHashMap<>());
            mapping.computeIfAbsent(annotation, k -> new ArrayList<>()).add(method);
          }
        }
      }

      return true;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private void build(Filer filer) throws Exception {
    Types typeUtils = processingEnv.getTypeUtils();
    Map<TypeElement, List<HandlerCompiler>> classes = new LinkedHashMap<>();
    for (Map.Entry<TypeElement, Map<TypeElement, List<ExecutableElement>>> e : routeMap
        .entrySet()) {
      TypeElement type = e.getKey();
      boolean isAbstract = type.getModifiers().contains(Modifier.ABSTRACT);
      /** Ignore abstract routes: */
      if (!isAbstract) {
        /** Expand route method from superclass(es): */
        Map<TypeElement, List<ExecutableElement>> mappings = e.getValue();
        for (TypeElement superType : superTypes(type)) {
          Map<TypeElement, List<ExecutableElement>> baseMappings = routeMap
              .getOrDefault(superType, Collections.emptyMap());
          for (Map.Entry<TypeElement, List<ExecutableElement>> be : baseMappings.entrySet()) {
            List<ExecutableElement> methods = mappings.get(be.getKey());
            if (methods == null) {
              mappings.put(be.getKey(), be.getValue());
            } else {
              for (ExecutableElement it : be.getValue()) {
                String signature = signature(it);
                if (!methods.stream().map(this::signature).anyMatch(signature::equals)) {
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
              HandlerCompiler compiler = new HandlerCompiler(processingEnv, type, method,
                  httpMethod, path);
              classes.computeIfAbsent(type, k -> new ArrayList<>())
                  .add(compiler);
            }
          }
        }
      }
    }

    List<String> moduleList = new ArrayList<>();
    for (Map.Entry<TypeElement, List<HandlerCompiler>> entry : classes.entrySet()) {
      TypeElement type = entry.getKey();
      String typeName = typeUtils.erasure(type.asType()).toString();
      List<HandlerCompiler> handlers = entry.getValue();
      ModuleCompiler module = new ModuleCompiler(processingEnv, typeName);
      String moduleClass = module.getModuleClass();
      byte[] moduleBin = module.compile(handlers);
      onClass(moduleClass, moduleBin);
      writeClass(filer.createClassFile(moduleClass, type), moduleBin);

      moduleList.add(moduleClass);
    }

    //doServices(filer, moduleList);
  }

  private String signature(ExecutableElement method) {
    return method.toString();
  }

  private List<TypeElement> superTypes(Element owner) {
    Types typeUtils = processingEnv.getTypeUtils();
    List<? extends TypeMirror> supertypes = typeUtils
        .directSupertypes(owner.asType());
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

  private void doServices(Filer filer, List<String> moduleList) throws IOException {
    String location = "META-INF/services/" + MvcFactory.class.getName();
    debug("%s", location);
    FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", location);
    StringBuilder content = new StringBuilder();
    for (String classname : moduleList) {
      debug("  %s", classname);
      content.append(classname).append(System.getProperty("line.separator"));
    }
    onResource(location, content.toString());
    try (PrintWriter writer = new PrintWriter(resource.openOutputStream())) {
      writer.println(content);
    }
  }

  protected void onClass(String className, byte[] bytecode) {
  }

  protected void onResource(String location, String content) {
  }

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
    List<String> path = path(annotation.getQualifiedName().toString(),
        annotation.getAnnotationMirrors());
    if (path.size() == 0) {
      path = path(annotation.getQualifiedName().toString(), exec.getAnnotationMirrors());
    }
    List<String> methodPath = path;
    if (prefix.size() == 0) {
      return path.isEmpty() ? Collections.singletonList("/") : path;
    }
    if (path.size() == 0) {
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
        .flatMap(mirror -> {
          String type = mirror.getAnnotationType().toString();
          if (Annotations.PATH.contains(type) || type.equals(method)) {
            return Stream.concat(Annotations.attribute(mirror, "path").stream(),
                Annotations.attribute(mirror, "value").stream());
          }
          return Stream.empty();
        })
        .distinct()
        .collect(Collectors.toList());
  }
}
