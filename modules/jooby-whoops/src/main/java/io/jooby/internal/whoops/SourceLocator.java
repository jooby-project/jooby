/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.whoops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceLocator {

  public static class Preview {
    private String code;

    private int line;

    public Preview(String code, int line) {
      this.code = code;
      this.line = line;
    }

    public int getLineStart() {
      return line;
    }

    public String getCode() {
      return code;
    }
  }

  public static class Source {
    private static final int[] RANGE = {0, 0};

    private final Path path;

    public Source(final Path path) {
      this.path = path;
    }

    public Path getPath() {
      return path;
    }

    public Preview preview(final int line, final int size) {
      List<String> lines = getLines();

      int[] range = range(line, size, lines.size());
      int from = range[0];
      int to = range[1];

      String code;
      if (from >= 0 && to <= lines.size()) {
        code = lines.subList(from, to).stream()
            .map(l -> l.length() == 0 ? " " : l)
            .collect(Collectors.joining("\n"));
      } else {
        code = "";
      }
      return new Preview(code, from + 1);
    }

    private int[] range(final int line, final int size, int totalSize) {
      if (line < totalSize) {
        int from = Math.max(line - size, 0);
        int toset = Math.max((line - from) - size, 0);
        int to = Math.min(from + toset + size * 2, totalSize);
        int fromset = Math.abs((to - line) - size);
        from = Math.max(from - fromset, 0);
        return new int[]{from, to};
      }
      return RANGE;
    }

    private List<String> getLines() {
      try {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
      } catch (IOException x) {
        return Collections.emptyList();
      }
    }

    @Override
    public String toString() {
      return path.toString();
    }
  }

  private Logger log = LoggerFactory.getLogger(getClass());

  private Path basedir;

  private Map<String, Source> sources = new ConcurrentHashMap<>();

  public SourceLocator(Path basedir) {
    this.basedir = basedir;
  }

  public Path getBasedir() {
    return basedir;
  }

  public Source source(String filename) {
    return sources.computeIfAbsent(filename, f -> {
      Set<String> skip = Stream.of("target", "bin", "build", "tmp", "temp", "node_modules", "node")
          .collect(Collectors.toSet());
      try {
        List<String> files = Arrays.asList(filename,
            filename.replace(".", File.separator) + ".java",
            filename.replace(".", File.separator) + ".kt",
            filename.replace(".", File.separator) + "Kt.kt");
        List<Path> source = new ArrayList<>();
        source.add(Paths.get(filename));
        log.debug("scanning {}", basedir);
        Files.walkFileTree(basedir, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(final Path dir,
              final BasicFileAttributes attrs) throws IOException {
            String dirName = dir.getFileName().toString();
            if (Files.isHidden(dir) || dirName.startsWith(".")) {
              log.debug("skipping hidden directory: {}", dir);
              return FileVisitResult.SKIP_SUBTREE;
            }
            if (skip.contains(dirName)) {
              log.debug("skipping binary directory: {}", dir);
              return FileVisitResult.SKIP_SUBTREE;
            }
            log.debug("found directory: {}", dir);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            return files.stream()
                .filter(f -> file.toString().endsWith(f))
                .findFirst()
                .map(f -> {
                  source.add(0, file.toAbsolutePath());
                  return FileVisitResult.TERMINATE;
                })
                .orElse(FileVisitResult.CONTINUE);
          }
        });
        return new Source(source.get(0));
      } catch (IOException x) {
        return new Source(Paths.get(filename));
      } finally {
        log.debug("done scanning {}", basedir);
      }
    });
  }
}
