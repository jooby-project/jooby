/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.whoops;

import java.io.BufferedReader;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Path path;

    public Source(final Path path) {
      this.path = path;
    }

    public Path getPath() {
      return path;
    }

    public Preview preview(final int line, final int size) {
      int start = Math.max(1, line - size);
      int end = line + size;
      List<String> codeLines = new ArrayList<>();
      int lineStart = -1;

      try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        String l;
        int n = 1;
        while ((l = reader.readLine()) != null) {
          if (n >= start) {
            if (lineStart == -1) {
              lineStart = n;
            }
            codeLines.add(l.length() == 0 ? " " : l);
          }
          if (n >= end) {
            break;
          }
          n++;
        }
      } catch (IOException x) {
        return new Preview("", 1);
      }

      if (lineStart == -1) {
        return new Preview("", 1);
      }

      return new Preview(String.join("\n", codeLines), lineStart);
    }

    @Override
    public String toString() {
      return path.toString();
    }
  }

  private Logger log = LoggerFactory.getLogger(getClass());

  private Path basedir;

  private Map<String, Source> sources = new ConcurrentHashMap<>();
  private final Map<String, List<Path>> sourceIndex = new ConcurrentHashMap<>();
  private volatile boolean indexBuilt = false;

  public SourceLocator(Path basedir) {
    this.basedir = basedir;
  }

  public Path getBasedir() {
    return basedir;
  }

  public Source source(String filename) {
    return sources.computeIfAbsent(
        filename,
        f -> {
          buildIndexIfNeeded();

          String simpleName = f.substring(f.lastIndexOf(File.separator) + 1);
          List<String> searchFiles =
              Arrays.asList(simpleName + ".java", simpleName + ".kt", simpleName + "Kt.kt");

          List<String> candidateSuffixes =
              Arrays.asList(f + ".java", f + ".kt", f.replace(".", File.separator) + "Kt.kt");

          for (String searchFile : searchFiles) {
            List<Path> paths = sourceIndex.get(searchFile);
            if (paths != null) {
              for (Path path : paths) {
                String pathStr = path.toString();
                for (String suffix : candidateSuffixes) {
                  if (pathStr.endsWith(suffix)) {
                    return new Source(path);
                  }
                }
              }
            }
          }
          return new Source(Paths.get(filename));
        });
  }

  private void buildIndexIfNeeded() {
    if (indexBuilt) {
      return;
    }
    synchronized (this) {
      if (indexBuilt) {
        return;
      }
      try {
        log.debug("scanning {}", basedir);
        Set<String> skip =
            Stream.of("target", "bin", "build", "tmp", "temp", "node_modules", "node")
                .collect(Collectors.toSet());
        Files.walkFileTree(
            basedir,
            new SimpleFileVisitor<Path>() {
              @Override
              public FileVisitResult preVisitDirectory(
                  final Path dir, final BasicFileAttributes attrs) throws IOException {
                String dirName = dir.getFileName().toString();
                if (Files.isHidden(dir) || dirName.startsWith(".")) {
                  log.debug("skipping hidden directory: {}", dir);
                  return FileVisitResult.SKIP_SUBTREE;
                }
                if (skip.contains(dirName)) {
                  log.debug("skipping binary directory: {}", dir);
                  return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
              }

              @Override
              public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith(".java") || fileName.endsWith(".kt")) {
                  sourceIndex.computeIfAbsent(fileName, k -> new ArrayList<>()).add(file);
                }
                return FileVisitResult.CONTINUE;
              }
            });
      } catch (IOException x) {
        log.error("source index failed", x);
      } finally {
        log.debug("done scanning {}", basedir);
        indexBuilt = true;
      }
    }
  }
}
