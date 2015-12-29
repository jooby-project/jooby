package org.jooby.routes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

public class RouteFinder {

  private Path basedir;

  public RouteFinder(final Path basedir) {
    Path src = basedir.resolve(Paths.get("src", "main", "java"));
    this.basedir = src.toFile().exists() ? src : basedir;
  }

  public void find() throws IOException {
    try (Stream<Path> files = Files.walk(basedir).filter(p -> p.toString().endsWith(".java"))) {
      Iterator<Path> it = files.iterator();
      while (it.hasNext()) {
        Path path = it.next();
      }
    }
  }

}
