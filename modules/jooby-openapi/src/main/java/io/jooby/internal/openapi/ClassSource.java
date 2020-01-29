package io.jooby.internal.openapi;

import io.jooby.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClassSource {
  private Path basedir;

  public ClassSource(Path basedir) {
    this.basedir = basedir;
  }

  public byte[] byteCode(String classname) {
    try {
      Path classpath = classpath(classname);
      return Files.readAllBytes(classpath);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private Path classpath(String classname) {
    Path path = basedir;
    String[] segments = classname.split("\\.");
    for (int i = 0; i < segments.length - 1; i++) {
      path = path.resolve(segments[i]);
    }
    path = path.resolve(segments[segments.length - 1] + ".class");
    return path;
  }
}
