/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.frontend;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.jooby.SneakyThrows.throwingRunnable;
import static java.util.Arrays.asList;

/**
 * Node task executor.
 *
 * @author edgar.
 * @since 2.8.0
 */
public interface NodeExecutor {
  enum Option {
    /**
     * Run a task once per JVM instance. Specially created for joobyRun to supports application
     * restart (hot reload) and long running node processes (like a web server).
     */
    ONCE {
      @Override Task toTask(Task next) {
        return (cmd, args) -> {
          String cmdline = cmdline(cmd, asList(args));
          String runkey = "frontend." + cmdline.replace(" ", ".");
          boolean run = Boolean.parseBoolean(System.getProperty(runkey, "true"));
          if (run) {
            try {
              System.setProperty(runkey, "false");
              next.execute(cmd, args);
            } finally {
              System.setProperty(runkey, "true");
            }
          }
        };
      }
    },

    /**
     * Run a task asynchronously (don't block the caller thread).
     */
    ASYNC {
      @Override Task toTask(Task next) {
        return (cmd, args) -> {
          String cmdline = cmdline(cmd, asList(args));
          Thread thread = new Thread(throwingRunnable(() -> next.execute(cmd, args)), cmdline);
          thread.setDaemon(true);
          thread.start();
        };
      }
    };

    static String cmdline(String cmd, List<String> strings) {
      return cmd + strings.stream().collect(Collectors.joining(" "));
    }

    abstract Task toTask(Task next);
  }

  /**
   * Task executor created from {@link Npm} or {@link Yarn}.
   *
   * @author edgar
   * @since 2.8.0
   */
  interface Task {

    /**
     * Execute the given command and wait for completion.
     *
     * @param cmd Command.
     * @param args Arguments.
     */
    void execute(String cmd, String... args);
  }

  /**
   * Creates a new task.
   *
   * @param options Task options.
   * @return A new task.
   */
  @Nonnull Task newTask(@Nonnull Set<Option> options);

  default void executeAsync(@Nonnull String cmd, String... args) {
    newTask(EnumSet.of(Option.ASYNC)).execute(cmd, args);
  }

  default void executeAsyncOnce(@Nonnull String cmd, String... args) {
    newTask(EnumSet.of(Option.ASYNC, Option.ONCE)).execute(cmd, args);
  }

  default void executeOnce(@Nonnull String cmd, String... args) {
    newTask(EnumSet.of(Option.ONCE)).execute(cmd, args);
  }

  /**
   * Execute a node task.
   *
   * @param cmd Command.
   * @param args Arguments.
   */
  default void execute(@Nonnull String cmd, String... args) {
    newTask(Collections.emptySet()).execute(cmd, args);
  }
}
