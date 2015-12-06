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
package org.jooby.hotreload;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.sun.nio.file.SensitivityWatchEventModifier;

/**
 * Example to watch a directory (or tree) for changes to files.
 */
public class Watcher {

  private final WatchService watcher;
  private volatile Map<WatchKey, Path> keys;
  private BiConsumer<Kind<?>, Path> listener;
  private Thread scanner;
  private volatile boolean stopped = false;

  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(final WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  public void start() {
    scanner.start();
  }

  public void stop() {
    stopped = true;
  }

  /**
   * Register the given directory with the WatchService
   */
  private void register(final Path dir) throws IOException {
    WatchKey key = dir.register(watcher, new Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY },
        SensitivityWatchEventModifier.HIGH);
    keys.put(key, dir);
  }

  /**
   * Register the given directory, and all its sub-directories, with the
   * WatchService.
   */
  private void registerAll(final Path start) throws IOException {
    // register directory and sub-directories
    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
          throws IOException {
        register(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public Watcher(final BiConsumer<Kind<?>, Path> listener, final Path... dirs)
      throws IOException {
    this.watcher = FileSystems.getDefault().newWatchService();
    this.keys = new HashMap<WatchKey, Path>();
    this.listener = listener;
    for (Path dir : dirs) {
      registerAll(dir);
    }

    this.scanner = new Thread(() -> {
      boolean process = true;
      while (!stopped && process) {
        process = processEvents();
      }
    }, "HotswapScanner");

    scanner.setDaemon(true);
  }

  private boolean processEvents() {
    // wait for key to be signalled
    WatchKey key;
    try {
      key = watcher.take();
    } catch (InterruptedException x) {
      return false;
    }

    Path dir = keys.get(key);
    if (dir == null) {
      return true;
    }

    for (WatchEvent<?> event : key.pollEvents()) {
      Kind<?> kind = event.kind();

      // TBD - provide example of how OVERFLOW event is handled
      if (kind == OVERFLOW) {
        continue;
      }

      // Context for directory entry event is the file name of entry
      WatchEvent<Path> ev = cast(event);
      Path name = ev.context();
      Path child = dir.resolve(name);
      File file = child.toFile();

      if (file.isFile()) {
        listener.accept(kind, dir.resolve(name));
      }

      // if directory is created, and watching recursively, then
      // register it and its sub-directories
      if (kind == ENTRY_CREATE) {
        try {
          if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
            registerAll(child);
          }
        } catch (IOException x) {
          // ignore to keep sample readbale
        }
      }
    }

    // reset key and remove from set if directory no longer accessible
    boolean valid = key.reset();
    if (!valid) {
      keys.remove(key);

      // all directories are inaccessible
      if (keys.isEmpty()) {
        return false;
      }
    }
    return true;
  }

}
