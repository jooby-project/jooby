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

    public MvcContext getContext() {
      return context;
    }

    @Override
    protected void onGeneratedSource(JavaFileObject source) {
      this.source = source;
    }
  }

  private final HookJoobyProcessor processor;

  public ProcessorRunner(Object instance) throws IOException {
    this(instance, Map.of());
  }

  public ProcessorRunner(Object instance, Map<String, Object> options) throws IOException {
    this.processor = new HookJoobyProcessor(System.out::println);
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
    var factoryClass = (Class<? extends MvcExtension>) classLoader.loadClass(factoryName);
    var constructor = factoryClass.getDeclaredConstructor();
    var extension = constructor.newInstance();
    var application = new Jooby();
    application.install(extension);
    consumer.accept(application, processor.getSource());
    return this;
  }

  public ProcessorRunner withSource(SneakyThrows.Consumer<JavaFileObject> consumer) {
    consumer.accept(processor.getSource());
    return this;
  }

  public ProcessorRunner withSource(boolean kt, SneakyThrows.Consumer<String> consumer)
      throws IOException {
    consumer.accept(processor.getContext().getRouters().get(0).toSourceCode(kt));
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
