/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.compiler;

import io.jooby.MvcModule;
import io.jooby.SneakyThrows;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MvcProcessor extends AbstractProcessor {

  private ProcessingEnvironment processingEnvironment;

  private List<String> moduleList = new ArrayList<>();

  @Override public Set<String> getSupportedAnnotationTypes() {
    return Annotations.HTTP_METHODS;
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public void init(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      if (roundEnv.processingOver()) {
        doServices(processingEnvironment.getFiler());
        return false;
      }
      /**
       * Do MVC handler: per each mvc method we create a Route.Handler.
       */
      Map<String, MvcHandlerCompiler> result = new HashMap<>();
      for (TypeElement httpMethod : annotations) {
        Set<? extends Element> methods = roundEnv.getElementsAnnotatedWith(httpMethod);
        for (Element e : methods) {
          ExecutableElement method = (ExecutableElement) e;
          List<String> paths = path(httpMethod, method);
          for (String path : paths) {
            MvcHandlerCompiler compiler = new MvcHandlerCompiler(processingEnvironment, method,
                httpMethod, path);
            onMvcHandler(compiler.getKey(), compiler);
            result.put(compiler.getKey(), compiler);
          }
        }
      }
      Filer filer = processingEnvironment.getFiler();
      Map<String, List<Map.Entry<String, MvcHandlerCompiler>>> classes = result.entrySet()
          .stream()
          .collect(Collectors.groupingBy(e -> e.getValue().getController().getName()));

      for (Map.Entry<String, List<Map.Entry<String, MvcHandlerCompiler>>> entry : classes
          .entrySet()) {

        List<Map.Entry<String, MvcHandlerCompiler>> handlers = entry.getValue();
        MvcModuleCompiler module = new MvcModuleCompiler(entry.getKey());
        String moduleClass = module.getModuleClass();
        byte[] moduleBin = module.compile(handlers);
        onClass(moduleClass, moduleBin);
        writeClass(filer.createClassFile(moduleClass), moduleBin);

        MvcModuleFactoryCompiler moduleFactoryCompiler = new MvcModuleFactoryCompiler(
            entry.getKey(), moduleClass);
        String factoryClass = moduleFactoryCompiler.getModuleFactoryClass();
        byte[] factoryBin = moduleFactoryCompiler.compile();
        writeClass(filer.createClassFile(factoryClass), factoryBin);
        moduleList.add(factoryClass);

        onClass(factoryClass, factoryBin);
        for (Map.Entry<String, MvcHandlerCompiler> handler : handlers) {
          String handleClass = handler.getValue().getGeneratedClass();
          byte[] handleBin = handler.getValue().compile();
          writeClass(filer.createClassFile(handleClass), handleBin);
          onClass(handleClass, handleBin);
        }
      }
      return true;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private void doServices(Filer filer) throws IOException {
    String location = "META-INF/services/" + MvcModule.class.getName();
    FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", location);
    String content = moduleList.stream().collect(Collectors.joining(System.getProperty("line.separator")));
    onResource(location, content);
    try (PrintWriter writer = new PrintWriter(resource.openOutputStream())) {
        writer.println(content);
    }
  }

  protected void onMvcHandler(String methodDescriptor, MvcHandlerCompiler compiler) {
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

  private List<String> path(TypeElement method, ExecutableElement exec) {
    List<String> prefix = path(exec.getEnclosingElement());
    // Favor GET("/path") over Path("/path") at method level
    List<String> path = path(method.getQualifiedName().toString(), method.getAnnotationMirrors());
    if (path.size() == 0) {
      path = path(method.getQualifiedName().toString(), exec.getAnnotationMirrors());
    }
    List<String> methodPath = path;
    if (prefix.size() == 0) {
      return path;
    }
    if (path.size() == 0) {
      return prefix;
    }
    return prefix.stream()
        .flatMap(root -> methodPath.stream().map(p -> root + p))
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
