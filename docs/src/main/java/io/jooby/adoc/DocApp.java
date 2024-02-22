/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import io.jooby.LoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.methvin.watcher.DirectoryWatcher;

public class DocApp extends Jooby {

  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  {
    ServerOptions server = new ServerOptions();
    server.setPort(4000);
    setServerOptions(server);

    Path site = DocGenerator.basedir().resolve("asciidoc").resolve("site");
    assets("/*", site);
  }

  public static void main(String[] args) throws Exception {
    // watch and block
    LoggingService.configure(DocApp.class.getClassLoader(), List.of("dev"));

    Path basedir = DocGenerator.basedir();

    Logger log = LoggerFactory.getLogger(DocGenerator.class);

    DocGenerator.generate(basedir, false, Arrays.asList(args).contains("v1"), true);

    runApp(args, DocApp::new);

    Path outdir = basedir.resolve("asciidoc").resolve("site");

    DirectoryWatcher watcher =
        DirectoryWatcher.builder()
            .path(basedir)
            .logger(NOP_LOGGER)
            .listener(
                event -> {
                  Path file = event.path();
                  if (!file.startsWith(outdir)) {
                      if (file.toString().endsWith(".js")
                          || file.toString().endsWith(".html")
                          || file.toString().endsWith(".adoc")) {
                        try {
                          DocGenerator.generate(
                              basedir, false, false, file.toString().endsWith(".adoc"));
                        } catch (Exception x) {
                          log.error("Site build resulted in exception", x);
                        }
                      }
                  }
                })
            .build();
    watcher.watch();
  }
}
