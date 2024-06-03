/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.tools.JavaFileObject;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import com.squareup.javapoet.JavaFile;
import io.jooby.*;

public class NewProcessorRunner {
  private final TestMvcSourceCodeProcessor processor = new TestMvcSourceCodeProcessor();

  public NewProcessorRunner(Object instance) throws IOException {
    this(instance, false);
  }

  public NewProcessorRunner(Object instance, boolean debug) throws IOException {
    Truth.assert_()
        .about(JavaSourcesSubjectFactory.javaSources())
        .that(sources(sourceNames(instance.getClass())))
        .withCompilerOptions("-Ajooby.debug=" + debug)
        .processedWith(processor)
        .compilesWithoutError();
  }

  public NewProcessorRunner withRouter(SneakyThrows.Consumer<Jooby> consumer) throws Exception {
    return withRouter((app, source) -> consumer.accept(app));
  }

  public NewProcessorRunner withRouter(SneakyThrows.Consumer2<Jooby, JavaFile> consumer)
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
