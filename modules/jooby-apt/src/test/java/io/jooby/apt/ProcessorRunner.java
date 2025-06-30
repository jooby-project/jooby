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
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.tools.JavaFileObject;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import io.jooby.*;
import io.jooby.internal.apt.MvcContext;

public class ProcessorRunner {

  private static class GeneratedSourceClassLoader extends ClassLoader {
    private final JavaFileObject classFile;
    private final String className;

    public GeneratedSourceClassLoader(ClassLoader parent, JavaFileObject source) {
      super(parent);
      this.classFile = javac().compile(List.of(source)).generatedFiles().get(0);
      this.className = source.getName().replace('/', '.').replace(".java", "");
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
    private JavaFileObject source;
    private String kotlinSource;

    public HookJoobyProcessor(Consumer<String> console) {
      super((kind, message) -> console.accept(message));
    }

    public GeneratedSourceClassLoader createClassLoader() {
      Objects.requireNonNull(source);
      return new GeneratedSourceClassLoader(getClass().getClassLoader(), source);
    }

    public JavaFileObject getSource() {
      return source;
    }

    public String getKotlinSource() {
      return kotlinSource;
    }

    public MvcContext getContext() {
      return context;
    }

    @Override
    protected void onGeneratedSource(JavaFileObject source) {
      this.source = source;
      try {
        // Generate kotlin source code inside the compiler scope... avoid false positive errors
        this.kotlinSource = context.getRouters().get(0).toSourceCode(true);
      } catch (IOException e) {
        SneakyThrows.propagate(e);
      }
    }
  }

  private final HookJoobyProcessor processor;

  public ProcessorRunner(Object instance) throws IOException {
    this(instance, Map.of());
  }

  public ProcessorRunner(Object instance, Consumer<String> stdout) throws IOException {
    this(instance, stdout, Map.of());
  }

  public ProcessorRunner(Object instance, Map<String, Object> options) throws IOException {
    this(instance, System.out::println, options);
  }

  public ProcessorRunner(Object instance, Consumer<String> stdout, Map<String, Object> options)
      throws IOException {
    this.processor = new HookJoobyProcessor(stdout::accept);
    var optionsArray =
        options.entrySet().stream().map(e -> "-A" + e.getKey() + "=" + e.getValue()).toList();
    Truth.assert_()
        .about(JavaSourcesSubjectFactory.javaSources())
        .that(sources(sourceNames(instance.getClass())))
        .withCompilerOptions(optionsArray.toArray(new String[0]))
        .processedWith(processor)
        .compilesWithoutError();
  }

  public ProcessorRunner withRouter(SneakyThrows.Consumer<Jooby> consumer) throws Exception {
    return withRouter((app, source) -> consumer.accept(app));
  }

  public ProcessorRunner withRouter(SneakyThrows.Consumer2<Jooby, JavaFileObject> consumer)
      throws Exception {
    var classLoader = processor.createClassLoader();
    var factoryName = classLoader.getClassName();
    var factoryClass = (Class<? extends Extension>) classLoader.loadClass(factoryName);
    var constructor = factoryClass.getDeclaredConstructor();
    var extension = constructor.newInstance();
    var application = new Jooby();
    application.install(extension);
    consumer.accept(application, processor.getSource());
    return this;
  }

  public ProcessorRunner withJavaObject(SneakyThrows.Consumer<JavaFileObject> consumer) {
    consumer.accept(processor.getSource());
    return this;
  }

  public ProcessorRunner withSourceCode(SneakyThrows.Consumer<String> consumer) {
    return withSourceCode(false, consumer);
  }

  public ProcessorRunner withSourceCode(boolean kt, SneakyThrows.Consumer<String> consumer) {
    consumer.accept(
        kt
            ? processor.kotlinSource
            : Optional.ofNullable(processor.getSource()).map(Objects::toString).orElse(null));
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
