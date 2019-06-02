/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Web server contract. Defines operations to start, join and stop a web server. Jooby comes
 * with three web server implementation: Jetty, Netty and Undertow.
 *
 * To use a web server, just add the dependency to your project.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Server {

  /**
   * Base class for server.
   */
  abstract class Base implements Server {

    private static final boolean useShutdownHook = Boolean
        .parseBoolean(System.getProperty("jooby.useShutdownHook", "true"));

    private AtomicBoolean stopping = new AtomicBoolean();

    protected void fireStart(List<Jooby> applications, Executor defaultWorker) {
      for (Jooby app : applications) {
        app.setDefaultWorker(defaultWorker).start(this);
      }
    }

    protected void fireReady(List<Jooby> applications) {
      for (Jooby app : applications) {
        app.ready(this);
      }
    }

    protected void fireStop(List<Jooby> applications) {
      if (stopping.compareAndSet(false, true)) {
        if (applications != null) {
          for (Jooby app : applications) {
            app.stop();
          }
        }
      }
    }

    protected void addShutdownHook() {
      if (useShutdownHook) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
      }
    }
  }

  /**
   * Set server options.
   *
   * @param options Server options.
   * @return This server.
   */
  @Nonnull Server setOptions(@Nonnull ServerOptions options);

  /**
   * Get server options.
   *
   * @return Server options.
   */
  @Nonnull ServerOptions getOptions();

  /**
   * Start an application.
   *
   * @param application Application to start.
   * @return This server.
   */
  @Nonnull Server start(Jooby application);

  /**
   * Block current thread so JVM doesn't exit.
   */
  @Nonnull default void join() {
    try {
      Thread.currentThread().join();
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Stop the server.
   *
   * @return Stop the server.
   */
  @Nonnull Server stop();

  /**
   * Test whenever the given exception is a connection-lost. Connection-lost exceptions don't
   * generate log.error statements.
   *
   * @param cause Exception to check.
   * @return True for connection lost.
   */
  static boolean connectionLost(@Nullable Throwable cause) {
    if (cause instanceof IOException) {
      String message = cause.getMessage();
      if (message != null) {
        String msg = message.toLowerCase();
        return msg.contains("reset by peer") || msg.contains("broken pipe");
      }
    }
    return (cause instanceof ClosedChannelException);
  }
}
