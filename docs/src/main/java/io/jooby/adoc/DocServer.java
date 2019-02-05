package io.jooby.adoc;

import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

public class DocServer {

  public static void main(String[] args) throws IOException {
    Path basedir = DocGenerator.basedir();

    DocGenerator.generate(basedir);

    System.out.println("listening for changes on: " + basedir);
    System.out.println("ready");

    ExecutorService executor = Executors.newSingleThreadExecutor();
    DirectoryWatcher watcher = DirectoryWatcher.builder()
        .path(basedir)
        .logger(NOP_LOGGER)
        .listener(event -> {
          Path file = event.path();
          if (file.toString().endsWith(".adoc")) {
            System.out.println("Change found: " + file);
            executor.execute(() -> {
              try {
                long start = System.currentTimeMillis();
                DocGenerator.generate(basedir);
                long end = System.currentTimeMillis();
                System.out.println("Sync ready in " + (end - start) + "ms");
              } catch (IOException x) {
                x.printStackTrace();
              }
            });
          }
        })
        .build();
    watcher.watch();
  }
}
