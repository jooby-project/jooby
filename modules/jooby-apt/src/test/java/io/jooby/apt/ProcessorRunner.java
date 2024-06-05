/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import com.squareup.javapoet.JavaFile;
import io.jooby.*;

public class ProcessorRunner {

  private static class GeneratedSourceClassLoader extends ClassLoader {
    private final JavaFileObject classFile;
    private final String className;

    public GeneratedSourceClassLoader(ClassLoader parent, JavaFile source) {
      super(parent);
      this.classFile = javac().compile(List.of(source.toJavaFileObject())).generatedFiles().get(0);
      this.className = source.packageName + "." + source.typeSpec.name;
    }

    public String getClassName() {
      return className;
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
      if (name.equals(className)) {
        try (var in = classFile.openInputStream()) {
          var bytes = in.readAllBytes();
          return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException c) {
          return super.findClass(name);
        }
      }
      return super.findClass(name);
    }
  }

  private static class HookJoobyProcessor extends JoobyProcessor {
    private JavaFile source;

    public HookJoobyProcessor() {
      super(new ConsoleMessager());
    }

    public GeneratedSourceClassLoader createClassLoader() {
      Objects.requireNonNull(source);
      return new GeneratedSourceClassLoader(getClass().getClassLoader(), source);
    }

    public JavaFile getSource() {
      return source;
    }

    @Override
    protected void onGeneratedSource(JavaFile source) {
      this.source = source;
    }
  }

  private static class ConsoleMessager implements Messager {
    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
      println(kind, msg);
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
      println(kind, msg, e);
    }

    @Override
    public void printMessage(
        Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
      println(kind, msg, e, " @", a);
    }

    @Override
    public void printMessage(
        Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
      println(kind, msg, e, " @", a, "=", v);
    }

    private void println(Diagnostic.Kind kind, CharSequence message, Object... args) {
      var out = kind == Diagnostic.Kind.ERROR ? System.err : System.out;
      out.println(
          kind
              + ": "
              + message
              + " "
              + Stream.of(args)
                  .filter(Objects::nonNull)
                  .map(Objects::toString)
                  .collect(Collectors.joining(" ")));
    }
  }

  private final HookJoobyProcessor processor = new HookJoobyProcessor();

  public ProcessorRunner(Object instance) throws IOException {
    this(instance, Map.of());
  }

  public ProcessorRunner(Object instance, Map<String, Object> options) throws IOException {
    var optionsArray =
        options.entrySet().stream().map(e -> "-A" + e.getKey() + "=" + e.getValue()).toList();
    Truth.assert_()
        .about(JavaSourcesSubjectFactory.javaSources())
        .that(sources(sourceNames(instance.getClass())))
        .withCompilerOptions(optionsArray.toArray(new String[0]))
        .processedWith(processor)
        .compilesWithoutError();
  }

  public ProcessorRunner(Object instance, boolean debug) throws IOException {
    this(instance, Map.of("jooby.debug", debug));
  }

  public ProcessorRunner withRouter(SneakyThrows.Consumer<Jooby> consumer) throws Exception {
    return withRouter((app, source) -> consumer.accept(app));
  }

  public ProcessorRunner withRouter(SneakyThrows.Consumer2<Jooby, JavaFile> consumer)
      throws Exception {
    var classLoader = processor.createClassLoader();
    var factoryName = classLoader.getClassName();
    var factoryClass = (Class<? extends MvcExtension>) classLoader.loadClass(factoryName);
    var constructor = factoryClass.getDeclaredConstructor();
    var extension = constructor.newInstance();
    var application = new Jooby();
    application.install(extension);
    consumer.accept(application, processor.getSource());
    return this;
  }

  private String[] sourceNames(Class input) {
    List<String> result = new ArrayList<>();
    while (input != Object.class) {
      result.add(input.getName());
      input = input.getSuperclass();
    }
    return result.toArray(new String[0]);
  }

  private static List<JavaFileObject> sources(String... names) throws IOException {
    Path basedir = basedir().resolve("src").resolve("test").resolve("java");
    List<JavaFileObject> sources = new ArrayList<>();
    for (String name : names) {
      String[] segments = name.split("\\.");
      Path path =
          Stream.of(segments)
              .limit(segments.length - 1)
              .reduce(basedir, Path::resolve, Path::resolve);
      path = path.resolve(segments[segments.length - 1] + ".java");
      assertTrue(path.toString(), Files.exists(path));
      sources.add(JavaFileObjects.forSourceString(name, Files.readString(path)));
    }
    return sources;
  }

  private static Path basedir() {
    Path path = Paths.get(System.getProperty("user.dir"));
    if (path.getFileName().toString().equals("jooby-apt")) {
      return path;
    }
    return path.resolve("modules").resolve("jooby-apt");
  }
}
