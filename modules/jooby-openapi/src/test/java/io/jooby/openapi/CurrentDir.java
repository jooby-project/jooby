/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class CurrentDir {
  public static Path basedir(String... others) {
    return basedir(List.of(others));
  }

  public static Path basedir(List<String> others) {
    var baseDir = Paths.get(System.getProperty("user.dir"));
    if (!baseDir.getFileName().toString().endsWith("jooby-openapi")) {
      baseDir = baseDir.resolve("modules").resolve("jooby-openapi");
    }
    for (var other : others) {
      baseDir = baseDir.resolve(other);
    }
    return baseDir;
  }

  public static Path testClass(Class<?> clazz, String file) {
    var packageDir = clazz.getPackage().getName().split("\\.");
    return basedir(
        Stream.concat(
                Stream.concat(Stream.of("src", "test", "resources"), Stream.of(packageDir)),
                Stream.of(file))
            .toList());
  }
}
