/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.compiler;

import com.google.auto.service.AutoService;
import io.jooby.SneakyThrows;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(Processor.class)
public class MvcProcessor implements Processor {

  Map<String, MvcHandlerCompiler> result = new HashMap<>();

  private Map<String, Object> modules = new HashMap<>();

  private ProcessingEnvironment processingEnvironment;

  @Override public Set<String> getSupportedOptions() {
    return Collections.emptySet();
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    return Annotations.HTTP_METHODS;
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override public void init(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnvironment) {
    if (annotations == null || annotations.size() == 0) {
      return false;
    }
    for (TypeElement httpMethod : annotations) {
      Set<? extends Element> methods = roundEnvironment.getElementsAnnotatedWith(httpMethod);
      for (Element e : methods) {
        ExecutableElement method = (ExecutableElement) e;
        List<String> paths = path(httpMethod, method);
        for (String path : paths) {
          MvcHandlerCompiler compiler = new MvcHandlerCompiler(processingEnvironment, method,
              httpMethod, path);
          result.put(compiler.getKey(), compiler);
        }
      }
    }
    Map<String, List<Map.Entry<String, MvcHandlerCompiler>>> classes = result.entrySet().stream()
        .collect(Collectors.groupingBy(e -> e.getValue().getController().getName()));
    for (Map.Entry<String, List<Map.Entry<String, MvcHandlerCompiler>>> entry : classes
        .entrySet()) {
      try {
        List<Map.Entry<String, MvcHandlerCompiler>> handlers = entry.getValue();
        MvcModuleCompiler module = new MvcModuleCompiler(entry.getKey());
        modules.put(entry.getKey() + "$Module", module.compile(handlers));
        for (Map.Entry<String, MvcHandlerCompiler> handler : handlers) {
          modules.put(handler.getValue().getGeneratedClass(), handler.getValue().compile());
        }
      } catch (Exception x) {
        x.printStackTrace();
      }
    }

    return true;
  }

  /*package*/ MvcHandlerCompiler compilerFor(String methodDescriptor) {
    return result.get(methodDescriptor);
  }

  /*package*/ ClassLoader getModuleClassLoader(boolean debug) {
    return new ClassLoader(getClass().getClassLoader()) {
      @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = (byte[]) modules.get(name);
        if (bytes != null) {
          if (debug) {
            System.out.println(MvcProcessor.this.toString(bytes));
          }
          return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
      }
    };
  }

  private String toString(byte[] bytes) {
    try {
      ClassReader reader = new ClassReader(bytes);
      ByteArrayOutputStream buff = new ByteArrayOutputStream();
      Printer printer = new ASMifier();
      TraceClassVisitor traceClassVisitor =
          new TraceClassVisitor(null, printer, new PrintWriter(buff));

      reader.accept(traceClassVisitor, ClassReader.SKIP_DEBUG);

      return new String(buff.toByteArray());
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
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
          if (type.equals(Annotations.PATH) || type.equals(method)) {
            return Stream.concat(Annotations.attribute(mirror, "path").stream(),
                Annotations.attribute(mirror, "value").stream());
          }
          return Stream.empty();
        })
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation,
      ExecutableElement member, String userText) {
    return Collections.emptyList();
  }
}
