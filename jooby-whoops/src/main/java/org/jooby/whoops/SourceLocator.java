/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.whoops;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import javaslang.control.Try;

interface SourceLocator {

  public class Source {
    private static final int[] RANGE = {0, 0 };
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
        return new int[]{from, to };
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

  static SourceLocator local() {
    return local(new File(System.getProperty("user.dir")).toPath());
  }

  static SourceLocator local(final Path path) {
    Path bin = path.resolve("bin");
    Path target = path.resolve("target");
    return filename -> Try.of(() -> {
      List<String> files = Arrays.asList(filename,
          filename.replace(".", File.separator) + ".java");
      List<Path> source = Lists.newArrayList(Paths.get(filename));
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
            throws IOException {
          if (dir.toFile().isHidden()) {
            return FileVisitResult.SKIP_SUBTREE;
          }
          if (dir.equals(bin) || dir.equals(target)) {
            return FileVisitResult.SKIP_SUBTREE;
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
            throws IOException {
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
      return new Source(source.get(0), Files.readAllLines(source.get(0), StandardCharsets.UTF_8));
    }).getOrElse(new Source(Paths.get(filename), Collections.emptyList()));
  };

  Source source(String filename);

}
