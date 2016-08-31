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
package org.jooby.assets;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.utils.MemoryManager;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import javaslang.control.Try;
import javaslang.control.Try.CheckedConsumer;

/**
 * Helper class that allows you extract and unpack a nodejs library from classpath.
 *
 * It provides a nice and safe nodejs execution environment.
 *
 * @author edgar
 *
 */
public class Nodejs {

  private static class Library implements Closeable {

    private Closeable closer;

    private Path root;

    private Library(final Path path) {
      this.closer = null;
      this.root = path;
    }

    private Library(final FileSystem fs) throws IOException {
      this.closer = fs;
      this.root = fs.getPath("/");
    }

    @Override
    public void close() throws IOException {
      if (closer != null) {
        closer.close();
      }
    }

    public Stream<Path> stream() throws IOException {
      return Files.walk(root);
    }

  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private ClassLoader loader;

  private File basedir;

  private NodeJS node;

  private MemoryManager scope;

  private Set<StandardCopyOption> coptions = Collections.emptySet();

  /**
   * Creates a new {@link Nodejs}.
   *
   * @param basedir Base dir where to deploy a library.
   * @param loader Class loader to use.
   */
  public Nodejs(final File basedir, final ClassLoader loader) {
    this.basedir = requireNonNull(basedir, "Basedir required.");
    this.loader = requireNonNull(loader, "ClassLoader required.");
    this.node = NodeJS.createNodeJS();
    V8 v8 = node.getRuntime();
    this.scope = new MemoryManager(v8);
  }

  /**
   * Creates a new {@link Nodejs}.
   *
   * @param basedir Base dir where to deploy a library.
   */
  public Nodejs(final File basedir) {
    this(basedir, Nodejs.class.getClassLoader());
  }

  /**
   * Creates a new {@link Nodejs} and use the <code>java.io.tmpdir</code> as base dir.
   */
  public Nodejs() {
    this(new File(System.getProperty("java.io.tmpdir")));
  }

  /**
   * Force to unpack library files every {@link #exec(String)} call. This is useful for development
   * and testing. By default files are unpacked just once (overwrite = false).
   *
   * @param overwrite True, to force overwrite.
   * @return
   */
  public Nodejs overwrite(final boolean overwrite) {
    if (overwrite) {
      this.coptions = EnumSet.of(StandardCopyOption.REPLACE_EXISTING);
    } else {
      this.coptions = Collections.emptySet();
    }
    return this;
  }

  /**
   * Unpack and execute a nodejs library. Once unpack this method will execute one of these scripts
   * in the following order: 1) [library].js; 2) main.js; 3) index.js.
   *
   * The first file found will be executed.
   *
   * @param library Library to unpack and execute this library.
   * @throws Throwable If something goes wrong.
   */
  public void exec(final String library) throws Throwable {
    exec(library, v8 -> {
    });
  }

  /**
   * Unpack and execute a nodejs library. Once unpack this method will execute one of these scripts
   * in the following order: 1) [library].js; 2) main.js; 3) index.js.
   *
   * The first file found will be executed.
   *
   * @param library Library to unpack and execute this library.
   * @throws Throwable If something goes wrong.
   */
  public void exec(final String library, final CheckedConsumer<V8> callback) throws Throwable {
    Path basedir = deploy(library);
    List<String> candidates = Arrays.asList(
        basedir.getFileName().toString() + ".js",
        "main.js",
        "index.js");

    Path main = candidates.stream()
        .map(it -> basedir.resolve(it))
        .filter(it -> it.toFile().exists())
        .findFirst()
        .orElseThrow(() -> new FileNotFoundException(candidates.toString()));

    callback.accept(node.getRuntime());

    node.exec(main.toFile());

    while (node.isRunning()) {
      node.handleMessage();
    }
  }

  /**
   * Release nodejs and v8 resources.
   */
  public void release() {
    Try.run(scope::release);
    node.release();
  }

  private Path deploy(final String library) throws Exception {
    URL url = loader.getResource(library);
    if (url == null) {
      throw new FileNotFoundException(library);
    }
    URI uri = url.toURI();
    log.debug("{}", uri);
    Path outdir = this.basedir.toPath().resolve("node_modules").resolve(library.replace("/", "."));
    Optional<Path> basedir = Try.of(() -> Paths.get(uri)).toJavaOptional();
    try (Library lib = loadLibrary(uri)) {
      try (Stream<Path> stream = lib.stream()) {
        stream.filter(it -> !Files.isDirectory(it))
            .forEach(it -> {
              String relative = basedir.map(d -> d.relativize(it).toString())
                  .orElse(it.toString().substring(1));
              Path output = outdir.resolve(relative);
              File fout = output.toFile();
              boolean copy = !fout.exists()
                  || coptions.contains(StandardCopyOption.REPLACE_EXISTING);
              if (copy) {
                log.debug("copying {} to {}", it, fout);
                fout.getParentFile().mkdirs();
                StandardCopyOption[] coptions = this.coptions
                    .toArray(new StandardCopyOption[this.coptions.size()]);

                Try.run(() -> Files.copy(it, output, coptions))
                    .onFailure(x -> log.error("can't copy {}", it, x));
              }
            });
      }
    }
    return outdir;
  }

  private Library loadLibrary(final URI lib) {
    return Try.of(() -> FileSystems.newFileSystem(lib, Maps.newHashMap()))
        .map(it -> Try.of(() -> new Library(it)).get())
        .recoverWith(x -> Try.of(() -> new Library(Paths.get(lib))))
        .get();
  }

  /**
   * Execute the given nodejs callback and automatically release v8 and nodejs resources.
   *
   * @param callback Nodejs callback.
   */
  public static void run(final CheckedConsumer<Nodejs> callback) {
    Nodejs node = new Nodejs();
    try {
      callback.accept(node);
    } catch (Throwable x) {
      throw Throwables.propagate(x);
    } finally {
      node.release();
    }
  }
}
