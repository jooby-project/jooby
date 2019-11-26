/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import io.jooby.Jooby;
import io.jooby.LogConfigurer;
import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

public class DocApp extends Jooby {

  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

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

    DocGenerator.generate(basedir, false, Arrays.asList(args).contains("v1"));

    log.info("doc ready");

    runApp(new String[]{"server.join=false", "server.port=4000"}, DocApp::new);

    DirectoryWatcher watcher = DirectoryWatcher.builder()
        .path(basedir.resolve("asciidoc"))
        .logger(NOP_LOGGER)
        .listener(event -> {
          Path file = event.path();
          if (file.toString().endsWith(".adoc")) {
            try {
              DocGenerator.generate(basedir, false, false);

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
