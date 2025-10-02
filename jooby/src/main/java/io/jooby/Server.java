/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

import java.io.EOFException;
import java.io.IOException;
import java.net.BindException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.exception.StartupException;
import io.jooby.internal.MutedServer;
import io.jooby.output.OutputFactory;

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
 * Server server = new NettyServer(); // or JettyServer or UndertowServer
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

    private final AtomicBoolean stopping = new AtomicBoolean();

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

    protected void fireStop(@Nullable List<Jooby> applications) {
      if (applications != null) {
        if (stopping.compareAndSet(false, true)) {
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

    private ServerOptions options = new ServerOptions();

    @Override
    public final ServerOptions getOptions() {
      return options;
    }

    @Override
    public Server setOptions(@NonNull ServerOptions options) {
      this.options = options;
      return this;
    }
  }

  default Server init(Jooby application) {
    return this;
  }

  OutputFactory getOutputFactory();

  /**
   * Set server options. This method should be called once, calling this method multiple times will
   * print a warning.
   *
   * @param options Server options.
   * @return This server.
   */
  Server setOptions(@NonNull ServerOptions options);

  /**
   * Get server name.
   *
   * @return Server name.
   */
  String getName();

  /**
   * Get server options.
   *
   * @return Server options.
   */
  ServerOptions getOptions();

  /**
   * Start an application.
   *
   * @param application Application to start.
   * @return This server.
   */
  Server start(@NonNull Jooby... application);

  /**
   * Utility method to turn off odd logger. This helps to ensure same startup log lines across
   * server implementations.
   *
   * <p>These logs are silent at application startup time.
   *
   * @return Name of the logs we want to temporarily silent.
   */
  default List<String> getLoggerOff() {
    return emptyList();
  }

  /**
   * Stop the server.
   *
   * @return Stop the server.
   */
  Server stop();

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
   * @return True address already in use.
   */
  static boolean isAddressInUse(@Nullable Throwable cause) {
    for (Predicate<Throwable> addressInUse : Base.addressInUseListeners) {
      if (addressInUse.test(cause)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Load server from classpath using {@link ServiceLoader}.
   *
   * @return A server.
   */
  static Server loadServer() {
    return loadServer(new ServerOptions());
  }

  /**
   * Load server from classpath using {@link ServiceLoader}.
   *
   * @param options Optional server options.
   * @return A server.
   */
  static Server loadServer(@NonNull ServerOptions options) {
    List<Server> servers =
        stream(
                spliteratorUnknownSize(
                    ServiceLoader.load(Server.class).iterator(), Spliterator.ORDERED),
                false)
            .toList();
    if (servers.isEmpty()) {
      throw new StartupException("Server not found.");
    }
    if (servers.size() > 1) {
      var names =
          servers.stream()
              .map(it -> it.getClass().getSimpleName().toLowerCase())
              .collect(Collectors.toList());
      var log = LoggerFactory.getLogger(servers.get(0).getClass());
      log.warn("Multiple servers found {}. Using: {}", names, names.get(0));
    }
    var server = servers.get(0);
    return server.setOptions(options);
  }
}
