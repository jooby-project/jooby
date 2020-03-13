/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import io.jooby.MvcFactory;
import io.jooby.SneakyThrows;
import io.jooby.annotations.Path;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

  private ProcessingEnvironment processingEnvironment;

  private List<String> moduleList = new ArrayList<>();

  private Set<TypeElement> pathAnnotations;
  private Set<TypeElement> httpAnnotations;

  final class MVCMethod {
    public ExecutableElement method;
    public TypeElement httpMethod;

    MVCMethod(ExecutableElement method, TypeElement httpMethod) {
      this.method = method;
      this.httpMethod = httpMethod;
    }
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    return new LinkedHashSet<String>() {{
      addAll(Annotations.HTTP_METHODS);
      addAll(Annotations.PATH);
    }};
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public void init(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;

    Elements eltUtil = processingEnvironment.getElementUtils();
    this.pathAnnotations = new LinkedHashSet<TypeElement>() {{
      for (String s: Annotations.PATH) {
        add(eltUtil.getTypeElement(s));
      }
    }};
    this.httpAnnotations = new LinkedHashSet<TypeElement>() {{
      for (String s: Annotations.HTTP_METHODS) {
        add(eltUtil.getTypeElement(s));
      }
    }};
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      if (roundEnv.processingOver()) {
        doServices(processingEnvironment.getFiler());
        return false;
      }

      JoobyProcessorRoundEnvironment joobyRoundEnv = new JoobyProcessorRoundEnvironment(roundEnv, processingEnvironment);

      Map<TypeElement, List<MVCMethod>> classMap = new HashMap<>();
      /**
       * Do MVC handler: per each mvc method we create a Route.Handler.
       */
      List<HandlerCompiler> result = new ArrayList<>();

      /**
       * If @Path annotation is present force inspecting all http mthods.
       */
      if (annotations.retainAll(this.pathAnnotations)) {
        annotations = httpAnnotations;
      }

      for (TypeElement httpMethod : annotations) {
        Set<? extends Element> methods = joobyRoundEnv.getElementsAnnotatedWith(httpMethod);
        for (Element e : methods) {
          ExecutableElement method = (ExecutableElement) e;
          TypeElement cls = (TypeElement) method.getEnclosingElement();
          TypeElement superCls = (TypeElement) ((DeclaredType) cls.getSuperclass()).asElement();
          superCls.getEnclosedElements();
          //System.out.println("\tANNOTATION: " + httpMethod + ", METHOD: " + method + ", CLS " + cls + ", <<" + superCls);
          if (!classMap.containsKey(cls)) {
            classMap.put(cls, new ArrayList<>());
          }
          List<String> paths = path(httpMethod, method);
          for (String path : paths) {
            HandlerCompiler compiler = new HandlerCompiler(processingEnvironment, method, httpMethod, path);
            result.add(compiler);
          }
          classMap.get(cls).add(new MVCMethod(method, httpMethod));
        }
      }

      Set<? extends Element> pathAnnotatedElements = roundEnv.getElementsAnnotatedWith(Path.class);
      for (Element c : pathAnnotatedElements) {
        if (c.getKind() == ElementKind.CLASS) {
          TypeElement newOwner = (TypeElement) c;
          TypeElement oldOwner = (TypeElement) ((DeclaredType) newOwner.getSuperclass()).asElement();
          //System.out.println("\tOWNER::" + oldOwner + " contained in " + classMap.keySet());
          if (classMap.containsKey(oldOwner)) {
            //System.out.println("\t\tCONTAINSKEY::" + oldOwner);
            for (MVCMethod e : classMap.get(oldOwner)) {
              //System.out.println("\t\t\tMVCMETHOD::" + e);
              for (String path : path(e.httpMethod, e.method, newOwner)) {
                HandlerCompiler compiler = new HandlerCompiler(processingEnvironment, e.method, newOwner, e.httpMethod, path);
                result.add(compiler);
              }
            }
          }
        }
      }

      Filer filer = processingEnvironment.getFiler();
      Map<String, List<HandlerCompiler>> classes = result.stream()
          .collect(Collectors.groupingBy(e -> e.getController().getName()));

      for (Map.Entry<String, List<HandlerCompiler>> entry : classes.entrySet()) {
        List<HandlerCompiler> handlers = entry.getValue();
        ModuleCompiler module = new ModuleCompiler(processingEnvironment, entry.getKey());
        String moduleClass = module.getModuleClass();
        byte[] moduleBin = module.compile(handlers);
        onClass(moduleClass, moduleBin);
        System.out.println("@moduleClass: " + moduleClass);
        writeClass(filer.createClassFile(moduleClass), moduleBin);

        moduleList.add(moduleClass);
      }
      return true;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private void doServices(Filer filer) throws IOException {
    String location = "META-INF/services/" + MvcFactory.class.getName();
    FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", location);
    String content = moduleList.stream()
        .collect(Collectors.joining(System.getProperty("line.separator")));
    onResource(location, content);
    try (PrintWriter writer = new PrintWriter(resource.openOutputStream())) {
      writer.println(content);
    }
  }

  protected void onMvcHandler(String methodDescriptor, HandlerCompiler compiler) {
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

  private List<String> path(TypeElement method, ExecutableElement exec, TypeElement owner) {
    List<String> prefix = path(owner);
    // Favor GET("/path") over Path("/path") at method level
    List<String> path = path(method.getQualifiedName().toString(), method.getAnnotationMirrors());
    if (path.size() == 0) {
      path = path(method.getQualifiedName().toString(), exec.getAnnotationMirrors());
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

  private List<String> path(TypeElement method, ExecutableElement exec) {
    return path(method, exec, (TypeElement) exec.getEnclosingElement());
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
