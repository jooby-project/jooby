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
        .path(basedir)
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
