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
    private final Map<String, JavaFileObject> classes = new LinkedHashMap<>();

    public GeneratedSourceClassLoader(ClassLoader parent, Map<String, JavaFileObject> sources) {
      super(parent);
      for (var e : sources.entrySet()) {
        classes.put(e.getKey(), javac().compile(List.of(e.getValue())).generatedFiles().get(0));
      }
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
      if (classes.containsKey(name)) {
        var classFile = classes.get(name);
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
    private Map<String, JavaFileObject> javaFiles = new LinkedHashMap<>();
    private Map<String, String> kotlinFiles = new LinkedHashMap<>();

    public HookJoobyProcessor(Consumer<String> console) {
      super((kind, message) -> console.accept(message));
    }

    public GeneratedSourceClassLoader createClassLoader() {
      return new GeneratedSourceClassLoader(getClass().getClassLoader(), javaFiles);
    }

    public JavaFileObject getSource() {
      return javaFiles.isEmpty() ? null : javaFiles.entrySet().iterator().next().getValue();
    }

    public String getKotlinSource() {
      return kotlinFiles.entrySet().iterator().next().getValue();
    }

    public MvcContext getContext() {
      return context;
    }

    @Override
    protected void onGeneratedSource(String classname, JavaFileObject source) {
      javaFiles.put(classname, source);
      try {
        // Generate kotlin source code inside the compiler scope... avoid false positive errors
        kotlinFiles.put(classname, context.getRouters().get(0).toSourceCode(true));
      } catch (IOException e) {
        SneakyThrows.propagate(e);
      }
    }
  }

  private final List<Object> instances;
  private final HookJoobyProcessor processor;

  public ProcessorRunner(Object instance) throws IOException {
    this(instance, Map.of());
  }

  public ProcessorRunner(List<Object> instances) throws IOException {
    this(instances, System.out::println, Map.of());
  }

  public ProcessorRunner(Object instance, Consumer<String> stdout) throws IOException {
    this(instance, stdout, Map.of());
  }

  public ProcessorRunner(Object instance, Map<String, Object> options) throws IOException {
    this(instance, System.out::println, options);
  }

  public ProcessorRunner(Object instance, Consumer<String> stdout, Map<String, Object> options)
      throws IOException {
    this(List.of(instance), stdout, options);
  }

  public ProcessorRunner(
      List<Object> instances, Consumer<String> stdout, Map<String, Object> options)
      throws IOException {
    this.instances = instances;
    this.processor = new HookJoobyProcessor(stdout);
    var optionsArray =
        options.entrySet().stream().map(e -> "-A" + e.getKey() + "=" + e.getValue()).toList();
    Truth.assert_()
        .about(JavaSourcesSubjectFactory.javaSources())
        .that(sources(sourceNames(instances.stream().map(Object::getClass).toList())))
        .withCompilerOptions(optionsArray.toArray(new String[0]))
        .processedWith(processor)
        .compilesWithoutError();
  }

  public ProcessorRunner withRouter(SneakyThrows.Consumer<Jooby> consumer) throws Exception {
    return withRouter((app, source) -> consumer.accept(app));
  }

  public ProcessorRunner withRouter(SneakyThrows.Consumer2<Jooby, JavaFileObject> consumer)
      throws Exception {
    return withRouter(instances.get(0).getClass(), consumer);
  }

  public ProcessorRunner withRouter(Class<?> routerType, SneakyThrows.Consumer<Jooby> consumer)
      throws Exception {
    return withRouter(routerType, (app, source) -> consumer.accept(app));
  }

  public ProcessorRunner withRouter(
      Class<?> routerType, SneakyThrows.Consumer2<Jooby, JavaFileObject> consumer)
      throws Exception {
    var classLoader = processor.createClassLoader();
    var factoryName = routerType.getName() + "_";
    var factoryClass = (Class<? extends Extension>) classLoader.loadClass(factoryName);
    Extension extension;
    try {
      var constructor = factoryClass.getDeclaredConstructor();
      extension = constructor.newInstance();
    } catch (NoSuchMethodException x) {
      var instance =
          instances.stream()
              .filter(it -> it.getClass().equals(routerType))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Not found: " + routerType));
      extension = factoryClass.getDeclaredConstructor(routerType).newInstance(instance);
    }

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
            ? processor.kotlinFiles.values().iterator().next()
            : Optional.ofNullable(processor.getSource()).map(Objects::toString).orElse(null));
    return this;
  }

  private String[] sourceNames(List<Class<? extends Object>> inputs) {
    List<String> result = new ArrayList<>();
    Set<Class> visited = new HashSet<>();
    inputs.stream()
        .forEach(
            input -> {
              while (input != Object.class && visited.add(input)) {
                result.add(input.getName());
                input = input.getSuperclass();
              }
            });
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
