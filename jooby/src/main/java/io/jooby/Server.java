/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.io.EOFException;
import java.io.IOException;
import java.net.BindException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.internal.MutedServer;

/**
 * Web server contract. Defines operations to start, join and stop a web server. Jooby comes with
 * three web server implementation: Jetty, Netty and Undertow.
 *
 * <p>A web server is automatically discovered using the Java Service Loader API. All you need is to
 * add the server dependency to your project classpath.
 *
 * <p>When service loader is not an option or do you want to manually bootstrap a server:
 *
 * <pre>{@code
 * Server server = new Netty(); // or Jetty or Utow
 *
 * App app = new App();
 *
 * server.start(app);
 *
 * ...
 *
 * server.stop();
 *
 * }</pre>
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Server {

  /** Base class for server. */
  abstract class Base implements Server {

    private static final Predicate<Throwable> CONNECTION_LOST =
        cause -> {
          if (cause instanceof IOException) {
            String message = cause.getMessage();
            if (message != null) {
              String msg = message.toLowerCase();
              return msg.contains("reset by peer")
                  || msg.contains("broken pipe")
                  || msg.contains("forcibly closed")
                  || msg.contains("connection reset");
            }
          }
          return (cause instanceof ClosedChannelException) || (cause instanceof EOFException);
        };

    private static final Predicate<Throwable> ADDRESS_IN_USE =
        cause ->
            (cause instanceof BindException)
                || (Optional.ofNullable(cause)
                    .map(Throwable::getMessage)
                    .map(String::toLowerCase)
                    .filter(msg -> msg.contains("address already in use"))
                    .isPresent());

    private static final List<Predicate<Throwable>> connectionLostListeners =
        new CopyOnWriteArrayList<>(singletonList(CONNECTION_LOST));

    private static final List<Predicate<Throwable>> addressInUseListeners =
        new CopyOnWriteArrayList<>(singletonList(ADDRESS_IN_USE));

    private static final boolean useShutdownHook =
        Boolean.parseBoolean(System.getProperty("jooby.useShutdownHook", "true"));

    private AtomicBoolean stopping = new AtomicBoolean();

    protected void fireStart(@NonNull List<Jooby> applications, @NonNull Executor defaultWorker) {
      for (Jooby app : applications) {
        app.setDefaultWorker(defaultWorker).start(this);
      }
    }

    protected void fireReady(@NonNull List<Jooby> applications) {
      for (Jooby app : applications) {
        app.ready(this);
      }
    }

    protected void fireStop(@NonNull List<Jooby> applications) {
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
        Runtime.getRuntime().addShutdownHook(new Thread(MutedServer.mute(this)::stop));
      }
    }
  }

  /**
   * Set server options.
   *
   * @param options Server options.
   * @return This server.
   */
  @NonNull Server setOptions(@NonNull ServerOptions options);

  @NonNull String getName();

  /**
   * Get server options.
   *
   * @return Server options.
   */
  @NonNull ServerOptions getOptions();

  /**
   * Start an application.
   *
   * @param application Application to start.
   * @return This server.
   */
  @NonNull Server start(@NonNull Jooby application);

  /**
   * Utility method to turn off odd logger. This help to ensure same startup log lines across server
   * implementations.
   *
   * <p>These logs are silent at application startup time.
   *
   * @return Name of the logs we want to temporarily silent.
   */
  default @NonNull List<String> getLoggerOff() {
    return emptyList();
  }

  /**
   * Block current thread.
   *
   * @deprecated This method is planned to be removed in next major release (3.x)
   */
  @Deprecated
  @NonNull default void join() {
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
  @NonNull Server stop();

  /**
   * Add a connection lost predicate. On unexpected exception, this method allows to customize which
   * error are considered connection lost. If the error is considered a connection lost, no log
   * statement will be emitted by the application.
   *
   * @param predicate Customize connection lost error.
   */
  static void addConnectionLost(@NonNull Predicate<Throwable> predicate) {
    Base.connectionLostListeners.add(predicate);
  }

  /**
   * Add an address in use predicate. On unexpected exception, this method allows to customize which
   * error are considered address in use. If the error is considered an address in use, no log
   * statement will be emitted by the application.
   *
   * @param predicate Customize connection lost error.
   */
  static void addAddressInUse(@NonNull Predicate<Throwable> predicate) {
    Base.addressInUseListeners.add(predicate);
  }

  /**
   * Test whenever the given exception is a connection-lost. Connection-lost exceptions don't
   * generate log.error statements.
   *
   * @param cause Exception to check.
   * @return True for connection lost.
   */
  static boolean connectionLost(@Nullable Throwable cause) {
    for (Predicate<Throwable> connectionLost : Base.connectionLostListeners) {
      if (connectionLost.test(cause)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Whenever the given exception is an address already in use. This probably won't work in none
   * English locale systems.
   *
   * @param cause Exception to check.
   * @return True address alaredy in use.
   */
  static boolean isAddressInUse(@Nullable Throwable cause) {
    for (Predicate<Throwable> addressInUse : Base.addressInUseListeners) {
      if (addressInUse.test(cause)) {
        return true;
      }
    }
    return false;
  }
}
