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
package org.jooby.crash;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.crsh.vfs.spi.AbstractFSDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import javaslang.Predicates;
import javaslang.Tuple2;
import javaslang.control.Try;

class CrashFSDriver extends AbstractFSDriver<Path> implements AutoCloseable {

  private static final String LOGIN = "login.groovy";

  private static Predicate<Path> FLOGIN = fname(LOGIN);

  private static final String JAR = ".jar!";

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Path root;

  private FileSystem fs;

  private Predicate<Path> filter;

  private URI src;

  private CrashFSDriver(final URI src, final FileSystem fs, final Predicate<Path> filter) {
    this.src = src;
    this.fs = fs;
    String path = src.toString();
    int jar = path.indexOf(JAR);
    path = path.substring(jar + JAR.length());
    this.root = fs.getPath(path);
    this.filter = filter;
  }

  private CrashFSDriver(final URI src, final Path root, final Predicate<Path> filter) {
    this.src = src;
    this.root = root;
    this.filter = filter;
  }

  @Override
  public Path root() throws IOException {
    return root;
  }

  @Override
  public String name(final Path handle) throws IOException {
    return Optional.ofNullable(handle.getFileName()).orElse(handle).toString().replaceAll("/", "");
  }

  @Override
  public boolean isDir(final Path handle) throws IOException {
    return Files.isDirectory(handle);
  }

  @Override
  public Iterable<Path> children(final Path handle) throws IOException {
    try (Stream<Path> walk = Files.walk(handle)) {
      List<Path> children = walk
          .skip(1)
          .filter(filter)
          .collect(Collectors.toList());
      return children;
    }
  }

  public boolean exists(final Predicate<Path> filter) {
    try (Stream<Path> walk = Try.of(() -> Files.walk(root)).getOrElse(Stream.empty())) {
      return walk
          .skip(1)
          .filter(filter)
          .findFirst()
          .isPresent();
    }
  }

  @Override
  public long getLastModified(final Path handle) throws IOException {
    return Try.of(() -> Files.getLastModifiedTime(handle).toMillis()).getOrElse(-1L);
  }

  @Override
  public Iterator<InputStream> open(final Path handle) throws IOException {
    return ImmutableList.of(Files.newInputStream(handle)).iterator();
  }

  @Override
  public void close() throws Exception {
    if (fs != null) {
      fs.close();
    }
  }

  @Override
  public String toString() {
    return root + " -> " + src;
  }

  public static List<CrashFSDriver> parse(final ClassLoader loader,
      final List<Tuple2<String, Predicate<Path>>> paths) {
    List<CrashFSDriver> drivers = new ArrayList<>();
    boolean loginFound = false;

    for (Tuple2<String, Predicate<Path>> path : paths) {
      for (URI uri : expandPath(loader, path._1)) {
        // classpath first
        CrashFSDriver driver = Try.of(() -> FileSystems.newFileSystem(uri, Collections.emptyMap()))
            .map(it -> Try.of(() -> new CrashFSDriver(uri, it, path._2)).get())
            // file system fallback
            .recoverWith(x -> Try.of(() -> new CrashFSDriver(uri, Paths.get(uri), path._2)))
            .get();
        // HACK to make sure login.groovy takes precedence over default one
        driver.log.debug("driver created: {}", driver);
        if (loginFound) {
          driver.filter = FLOGIN.negate().and(driver.filter);
        } else if (driver.exists(FLOGIN)) {
          driver.log.debug("  login.groovy found: {}", driver);
          loginFound = true;
        }
        drivers.add(driver);
      }
    }
    return drivers;
  }

  private static List<URI> expandPath(final ClassLoader loader, final String pattern) {
    List<URI> result = new ArrayList<>();
    File file = new File(pattern);
    if (file.exists()) {
      result.add(file.toURI());
    }
    Try.run(() -> {
      Collections.list(loader.getResources(pattern))
          .stream()
          .map(it -> Try.of(() -> it.toURI()).get())
          .forEach(result::add);
    });
    return result;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  public static Predicate<Path> noneOf(final String... names) {
    Predicate[] predicates = new Predicate[names.length];
    for (int i = 0; i < predicates.length; i++) {
      predicates[i] = fname(names[i]);
    }
    return Predicates.noneOf(predicates);
  }

  public static Predicate<Path> fname(final String name) {
    return it -> Optional.ofNullable(it.getFileName())
        .map(Object::toString)
        .orElse(it.toString())
        .equals(name);
  }

  public static Predicate<Path> endsWith(final String ext) {
    return it -> Optional.ofNullable(it.getFileName())
        .map(Object::toString)
        .orElse(it.toString())
        .endsWith(ext);
  }

}
