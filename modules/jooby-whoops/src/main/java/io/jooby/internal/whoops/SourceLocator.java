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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceLocator {

  public static class Source {
    private static final int[] RANGE = {0, 0};
    private final Path path;

    private final List<String> lines;

    public Source(final Path path, final List<String> lines) {
      this.path = path;
      this.lines = lines;
    }

    public Path getPath() {
      return path;
    }

    public List<String> getLines() {
      return lines;
    }

    public int[] range(final int line, final int size) {
      if (line < lines.size()) {
        int from = Math.max(line - size, 0);
        int toset = Math.max((line - from) - size, 0);
        int to = Math.min(from + toset + size * 2, lines.size());
        int fromset = Math.abs((to - line) - size);
        from = Math.max(from - fromset, 0);
        return new int[]{from, to};
      }
      return RANGE;
    }

    public String source(final int from, final int to) {
      if (from >= 0 && to <= lines.size()) {
        return lines.subList(from, to).stream()
            .map(l -> l.length() == 0 ? " " : l)
            .collect(Collectors.joining("\n"));
      }
      return "";
    }

    @Override
    public String toString() {
      return path.toString();
    }
  }

  private Logger log = LoggerFactory.getLogger(getClass());
  private Path basedir;

  private ClassLoader classLoader;

  public SourceLocator(Path basedir, ClassLoader classLoader) {
    this.basedir = basedir;
    this.classLoader = classLoader;
  }

  public Path getBasedir() {
    return basedir;
  }

  public Source source(String filename) {
    Set<String> skip = Stream.of("target", "bin", "build", "tmp", "temp", "node_modules", "node")
        .collect(Collectors.toSet());
    try {
      List<String> files = Arrays.asList(filename,
          filename.replace(".", File.separator) + ".java");
      List<Path> source = new ArrayList<>(Arrays.asList(Paths.get(filename)));
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

      return new Source(source.get(0),
          Files.readAllLines(source.get(0), StandardCharsets.UTF_8));
    } catch (IOException x) {
      return new Source(Paths.get(filename), Collections.emptyList());
    } finally {
      log.debug("done scanning {}", basedir);
    }
  }

  public Optional<Class> findClass(final String name) {
    return Stream.of(classLoader, Thread.currentThread().getContextClassLoader())
        // we don't care about exception
        .flatMap(loader -> {
          try {
            return Stream.of(loader.loadClass(name));
          } catch (ClassNotFoundException x) {
            return Stream.<Class>empty();
          }
        })
        .findFirst();
  }

  public String locationOf(final Class clazz) {
    return Optional.ofNullable(clazz.getResource(clazz.getSimpleName() + ".class"))
        .map(url -> {
          try {
            String path = url.getPath();
            int i = path.indexOf("!");
            if (i > 0) {
              // jar url
              String jar = path.substring(0, i);
              return jar.substring(Math.max(jar.lastIndexOf('/'), -1) + 1);
            }
            String classfile = clazz.getName().replace(".", "/") + ".class";
            String relativePath = path.replace(classfile, "");
            return basedir
                .relativize(Paths.get(relativePath).toFile().getCanonicalFile().toPath())
                .toString();
          } catch (Exception x) {
            return "~unknown";
          }
        }).orElse("~unknown");
  }
}
