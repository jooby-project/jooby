/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.utow;

import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.internal.utow.UtowHandler;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.encoding.EncodingHandler;
import org.xnio.Options;

import javax.annotation.Nonnull;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Web server implementation using <a href="http://undertow.io/">Undertow</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class Utow extends Server.Base {

  private static final int BACKLOG = 8192;

  private Undertow server;

  private List<Jooby> applications = new ArrayList<>();

  private ServerOptions options = new ServerOptions()
      .setIoThreads(ServerOptions.IO_THREADS)
      .setServer("utow");

  @Nonnull @Override public Utow setOptions(@Nonnull ServerOptions options) {
    this.options = options
        .setIoThreads(options.getIoThreads());
    return this;
  }

  @Nonnull @Override public ServerOptions getOptions() {
    return options;
  }

  @Override public Server start(@Nonnull Jooby application) {
    try {
      applications.add(application);

      addShutdownHook();

      HttpHandler handler = new UtowHandler(applications.get(0), options.getBufferSize(),
          options.getMaxRequestSize(),
          options.isDefaultHeaders());

      if (options.isGzip()) {
        handler = new EncodingHandler.Builder().build(null).wrap(handler);
      }

      server = Undertow.builder()
          .addHttpListener(options.getPort(), "0.0.0.0")
          .setBufferSize(options.getBufferSize())
          /** Socket : */
          .setSocketOption(Options.BACKLOG, BACKLOG)
          /** Server: */
          // HTTP/1.1 is keep-alive by default, turn this option off
          .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
          .setServerOption(UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, true)
          .setServerOption(UndertowOptions.ALWAYS_SET_DATE, options.isDefaultHeaders())
          .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
          .setServerOption(UndertowOptions.DECODE_URL, false)
          /** Worker: */
          .setIoThreads(options.getIoThreads())
          .setWorkerOption(Options.WORKER_NAME, "application")
          .setWorkerThreads(options.getWorkerThreads())
          .setHandler(handler)
          .build();

      server.start();
      // NOT IDEAL, but we need to fire onStart after server.start to get access to Worker
      fireStart(applications, server.getWorker());

      fireReady(Collections.singletonList(application));

      return this;
    } catch (RuntimeException x) {
      Throwable cause = Optional.ofNullable(x.getCause()).orElse(x);
      if (cause instanceof BindException) {
        cause = new BindException("Address already in use: " + options.getPort());
      }
      throw SneakyThrows.propagate(cause);
    }
  }

  @Nonnull @Override public Server stop() {
    fireStop(applications);
    applications = null;
    if (server != null) {
      server.stop();
      server = null;
    }
    return this;
  }

}
