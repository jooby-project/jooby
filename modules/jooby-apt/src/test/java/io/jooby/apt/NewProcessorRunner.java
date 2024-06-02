/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
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
import io.jooby.internal.newapt.ConsoleMessager;

public class NewProcessorRunner {
  public NewProcessorRunner(Object instance) throws Exception {
    this(instance, false);
  }

  public NewProcessorRunner(Object instance, boolean debug) throws Exception {
    Truth.assert_()
        .about(JavaSourcesSubjectFactory.javaSources())
        .that(sources(sourceNames(instance.getClass())))
        .withCompilerOptions("-Ajooby.debug=" + debug)
        .processedWith(new MvcSourceCodeProcessor(new ConsoleMessager()))
        .compilesWithoutError();
  }

  private String[] sourceNames(Class input) {
    List<String> result = new ArrayList<>();
    while (input != Object.class) {
      result.add(input.getName());
      input = input.getSuperclass();
    }
    return result.toArray(new String[0]);
  }

  private static List<JavaFileObject> sources(String... names) throws MalformedURLException {
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
      sources.add(JavaFileObjects.forResource(path.toUri().toURL()));
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
