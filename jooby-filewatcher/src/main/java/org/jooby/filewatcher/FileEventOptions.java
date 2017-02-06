/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.filewatcher;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Objects.requireNonNull;

/**
 * Allow to customize a file watch handler. You can listen for particular event kinds, apply a glob
 * filter, recursive listening for changes and set watcher priority or modifier.
 * <p>
 * Default filter watch recursively listen for all the available kinds using a <code>HIGH</code>
 * modifier.
 * <p>
 * This class can't be instantiated by client code. Intances of this class are provided via
 * configuration callbacks. See
 * {@link FileWatcher#register(Path, Class, java.util.function.Consumer)}
 *
 * @author edgar
 * @since 1.1.0
 */
public class FileEventOptions {

  private List<WatchEvent.Kind<Path>> kinds = new ArrayList<>();

  static final PathMatcher TRUE = new PathMatcher() {

    @Override
    public boolean matches(final Path path) {
      return true;
    }

    @Override
    public String toString() {
      return "**/*";
    }
  };

  private static final WatchEvent.Kind<?>[] KINDS = {ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY};

  private final List<PathMatcher> matchers = new ArrayList<>();

  private Modifier modifier = SensitivityWatchEventModifier.HIGH;

  private boolean recursive = true;

  private final Class<? extends FileEventHandler> handler;

  private final Path path;

  private FileEventHandler handlerInstance;

  FileEventOptions(final Path path, final Class<? extends FileEventHandler> handler)
      throws IOException {
    if (!Files.exists(path)) {
      Files.createDirectories(path);
    }
    this.path = path;
    this.handler = handler;
    this.handlerInstance = null;
  }

  FileEventOptions(final Path path, final FileEventHandler handler) throws IOException {
    this(path, handler.getClass());
    this.handlerInstance = handler;
  }

  /**
   * Append a kind filter.
   * <p>
   * The default filter is: {@link StandardWatchEventKinds#ENTRY_CREATE},
   * {@link StandardWatchEventKinds#ENTRY_DELETE} and {@link StandardWatchEventKinds#ENTRY_MODIFY}.
   *
   * @param kind Filter type.
   * @return This options.
   */
  public FileEventOptions kind(final WatchEvent.Kind<Path> kind) {
    requireNonNull(kind, "WatchEvent.Kind required.");
    kinds.add(kind);
    return this;
  }

  /**
   * Turn on/off watching of subfolder.
   *
   * @param recursive True to watch on subfolder.
   * @return This options.
   */
  public FileEventOptions recursive(final boolean recursive) {
    this.recursive = recursive;
    return this;
  }

  /**
   * Add a path filter using <code>glob</code> expression, like <code><pre>** / *.java</pre><code>,
   * etc...
   *
   * @param expression Glob expression.
   * @return This options.
   */
  public FileEventOptions includes(final String expression) {
    requireNonNull(expression, "Glob expression required.");
    this.matchers.add(new GlobPathMatcher(expression));
    return this;
  }

  /**
   * Set a watch modifier. Default is: <code>HIGH</code>.
   *
   * @param modifier Watch modifier.
   * @return This options.
   */
  public FileEventOptions modifier(final WatchEvent.Modifier modifier) {
    requireNonNull(modifier, "Modifier required.");
    this.modifier = modifier;
    return this;
  }

  Modifier modifier() {
    return modifier;
  }

  WatchEvent.Kind<?>[] kinds() {
    return kinds.size() == 0 ? KINDS : kinds.toArray(new WatchEvent.Kind[0]);
  }

  PathMatcher filter() {
    return matchers.size() == 0 ? TRUE : new FirstOfPathMatcher(matchers);
  }

  boolean recursive() {
    return recursive;
  }

  Path path() {
    return path;
  }

  FileEventHandler handler(
      final Function<Class<? extends FileEventHandler>, FileEventHandler> factory) {
    return Optional.ofNullable(handlerInstance).orElseGet(() -> factory.apply(handler));
  }

  @Override
  public String toString() {
    return path + " {kinds: " + Arrays.toString(kinds()) + ", filter: " + filter()
        + ", recursive: " + recursive + ", modifier: " + modifier + "}";
  }
}
