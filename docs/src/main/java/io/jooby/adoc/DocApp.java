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

import io.jooby.Jooby;
import io.jooby.LogConfigurer;
import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;

import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

public class DocApp extends Jooby {
  {
    Path site = DocGenerator.basedir().resolve("asciidoc").resolve("site");
    assets("/*", site);
  }

  public static void main(String[] args) throws Exception {
    // watch and block
    LogConfigurer.configure(Arrays.asList("dev"));

    Path basedir = DocGenerator.basedir();

    Logger log = LoggerFactory.getLogger(DocGenerator.class);

    log.info("waiting for doc");

    DocGenerator.generate(basedir, false);

    log.info("doc ready");

    runApp(new String[]{"server.join=false"}, DocApp::new);

    DirectoryWatcher watcher = DirectoryWatcher.builder()
        .path(basedir.resolve("asciidoc"))
        .logger(NOP_LOGGER)
        .listener(event -> {
          Path file = event.path();
          if (file.toString().endsWith(".adoc")) {
            try {
              DocGenerator.generate(basedir, false);

              log.info("doc ready");
            } catch (Exception x) {
              log.error("doc sync error", x);
            }
          }
        })
        .build();
    watcher.watch();
  }
}
