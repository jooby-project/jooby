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
package org.jooby.filewatcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

class FileMonitor implements Runnable {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Injector injector;

  private final WatchService watcher;

  private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();

  private final Map<Path, FileEventOptions> options = new ConcurrentHashMap<>();

  private final Set<FileEventOptions> optionList;

  @Inject
  public FileMonitor(final Injector injector,
      final WatchService watcher, final Set<FileEventOptions> optionList) throws IOException {
    this.injector = injector;
    this.watcher = watcher;
    this.optionList = optionList;

    // start monitor:
    Thread thread = new Thread(this, "file-watcher");
    thread.setDaemon(true);
    thread.start();
  }

  @Override
  public void run() {
    // register directory:
    for (FileEventOptions options : optionList) {
      log.debug("watching: {}", options);
      try {
        registerTree(options.path(), options);
      } catch (IOException x) {
        log.error("traversal of {} resulted in exception", options.path(), x);
      }
    }

    boolean process = true;
    while (process) {
      try {
        process = poll(watcher.take());
      } catch (InterruptedException x) {
        Thread.currentThread().interrupt();
        process = false;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private boolean poll(final WatchKey key) {
    Path source = keys.get(key);
    if (source == null) {
      log.debug("ignoring key {}", key);
      return true;
    }

    for (WatchEvent<?> event : key.pollEvents()) {
      Kind<?> kind = event.kind();

      if (kind != OVERFLOW) {

        // Context for directory entry event is the file name of entry
        Path path = source.resolve((Path) event.context()).toAbsolutePath();
        log.debug("found {}({})", path, kind);

        // handler
        FileEventOptions options = this.options.get(source);
        PathMatcher filter = options.filter();
        if (filter.matches(path)) {
          FileEventHandler handler = options.handler(injector::getInstance);
          try {
            handler.handle((Kind<Path>) kind, path);
          } catch (IOException x) {
            log.debug("execution of {}({}) resulted in exception", path, kind, x);
          }
        } else {
          log.debug("ignoring {}({})", kind, path);
        }

        // if directory is created, and watching recursively, then
        // register it and its sub-directories
        if (options.recursive() && kind == ENTRY_CREATE && Files.isDirectory(path)) {
          try {
            registerTree(path, options);
          } catch (IOException x) {
            log.debug("execution of {}({}) resulted in exception", path, kind, x);
          }
        }
      } else {
        log.trace("execution of {} resulted in {}", event.context(), kind);
      }
    }

    // reset key and remove from set if directory no longer accessible
    if (!key.reset()) {
      keys.remove(key);
    }
    return !keys.isEmpty();
  }

  private void register(final Path path, final FileEventOptions options) throws IOException {
    Kind<?>[] kinds = options.kinds();
    Modifier modifier = options.modifier();
    WatchKey key = path.register(watcher, kinds, modifier);
    this.keys.put(key, path);
    this.options.put(path, options);
  }

  private void registerTree(final Path path, final FileEventOptions options) throws IOException {
    if (options.recursive()) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
            throws IOException {
          register(dir, options);
          return FileVisitResult.CONTINUE;
        }
      });
    } else {
      register(path, options);
    }
  }

}
