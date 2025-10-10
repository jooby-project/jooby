/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;

public class TestUtil {

  public static final Path File_19kb = userdir("src", "test", "resources", "files", "19kb.txt");
  public static final String _19kb = readText(File_19kb);

  public static final String _16kb = _19kb.substring(0, ServerOptions._16KB);

  public static final String _8kb = _16kb.substring(0, _16kb.length() / 2);

  private static String readText(Path file) {
    try {
      return Files.readString(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public static Path userdir(String... segments) {
    Path path = Paths.get(System.getProperty("user.dir"));
    for (String segment : segments) {
      path = path.resolve(segment);
    }
    return path;
  }
}
