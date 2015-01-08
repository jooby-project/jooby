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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal.hotswap;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.nio.file.SensitivityWatchEventModifier;
import com.typesafe.config.Config;

public class HotswapScanner {

  public interface Listener {
    void changed(String resource);
  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private WatchService watcher;

  private volatile boolean stopped = false;

  private HashMap<WatchKey, Path> keys;

  private Thread scanner;

  private Listener listener;

  private Path buildir;

  public HotswapScanner(final Listener listener, final Config config) throws IOException {
    this.listener = requireNonNull(listener, "The listener is required.");
    this.watcher = FileSystems.getDefault().newWatchService();
    this.keys = new HashMap<WatchKey, Path>();
    this.buildir = Paths.get(config.getString("application.builddir"));
    registerAll(buildir);

    this.scanner = new Thread(() -> {
      boolean process = true;
      while (!stopped && process) {
        process = processEvents();
      }
    }, getClass().getSimpleName());
    scanner.setDaemon(true);
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
    WatchKey key = dir.register(watcher, new WatchEvent.Kind[]{
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE },
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

  /**
   * Process all events for keys queued to the watcher
   */
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

      if (kind == OVERFLOW) {
        log.warn("overflow found: {}", key);
        continue;
      }

      // Context for directory entry event is the file name of entry
      Path child = dir.resolve((Path) event.context());
      boolean isdir = Files.isDirectory(child, NOFOLLOW_LINKS);

      if (!isdir) {
        listener.changed(child.subpath(buildir.getNameCount(), child.getNameCount()).toString());
      }

      // if directory is created, and watching recursively, then
      // register it and its sub-directories
      if (kind == ENTRY_CREATE) {
        try {
          if (isdir) {
            registerAll(child);
          }
        } catch (IOException ex) {
          log.debug("ignoring dir", ex);
        }
      }
    }

    // reset key and remove from set if directory no longer accessible
    if (!key.reset()) {
      keys.remove(key);

      // all directories are inaccessible
      if (keys.isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
