/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import io.methvin.watcher.DirectoryWatcher;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

public class DocServer {

  public static void main(String[] args) throws Exception {
    Path basedir = DocGenerator.basedir();

    DocGenerator.generate(basedir, false);

    Process process = new ProcessBuilder("open", basedir.resolve("out").resolve("index.html").toUri().toString())
        .start();
    process.waitFor();
    process.destroy();

    System.out.println("listening for changes on: " + basedir);

    ExecutorService executor = Executors.newSingleThreadExecutor();
    DirectoryWatcher watcher = DirectoryWatcher.builder()
        .path(basedir.resolve("asciidoc"))
        .logger(NOP_LOGGER)
        .listener(event -> {
          Path file = event.path();
          if (file.toString().endsWith(".adoc")) {
            System.out.println("Change found: " + file);
            executor.execute(() -> {
              try {
                long start = System.currentTimeMillis();
                DocGenerator.generate(basedir, false);
                long end = System.currentTimeMillis();
                System.out.println("Sync ready in " + (end - start) + "ms");
              } catch (Exception x) {
                x.printStackTrace();
              }
            });
          }
        })
        .build();
    watcher.watch();
  }
}
