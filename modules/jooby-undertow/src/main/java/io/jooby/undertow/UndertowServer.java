/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.undertow;

import static io.undertow.UndertowOptions.ENABLE_HTTP2;

import java.net.BindException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.SslClientAuthMode;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.SslOptions;
import io.jooby.exception.StartupException;
import io.jooby.internal.undertow.UndertowHandler;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.HttpContinueReadHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;

/**
 * Web server implementation using <a href="http://undertow.io/">Undertow</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class UndertowServer extends Server.Base {

  private static final int BACKLOG = 8192;

  private static final int _100 = 100;

  private static final int _10 = 10;

  private Undertow server;

  private List<Jooby> applications = new ArrayList<>();

  private ServerOptions options =
      new ServerOptions().setIoThreads(ServerOptions.IO_THREADS).setServer("utow");

  @NonNull @Override
  public UndertowServer setOptions(@NonNull ServerOptions options) {
    this.options = options.setIoThreads(options.getIoThreads());
    return this;
  }

  @NonNull @Override
  public ServerOptions getOptions() {
    return options;
  }

  @NonNull @Override
  public String getName() {
    return "undertow";
  }

  @Override
  public Server start(@NonNull Jooby application) {
    try {
      applications.add(application);

      addShutdownHook();

      HttpHandler handler =
          new UndertowHandler(
              applications.get(0),
              options.getBufferSize(),
              options.getMaxRequestSize(),
              options.getDefaultHeaders());

      if (options.getCompressionLevel() != null) {
        int compressionLevel = options.getCompressionLevel();
        handler =
            new EncodingHandler(
                handler,
                new ContentEncodingRepository()
                    .addEncodingHandler("gzip", new GzipEncodingProvider(compressionLevel), _100)
                    .addEncodingHandler(
                        "deflate", new DeflateEncodingProvider(compressionLevel), _10));
      }

      if (options.isExpectContinue() == Boolean.TRUE) {
        handler = new HttpContinueReadHandler(handler);
      }

      Undertow.Builder builder =
          Undertow.builder()
              .setBufferSize(options.getBufferSize())
              /** Socket : */
              .setSocketOption(Options.BACKLOG, BACKLOG)
              /** Server: */
              // HTTP/1.1 is keep-alive by default, turn this option off
              .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
              .setServerOption(UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, true)
              .setServerOption(UndertowOptions.ALWAYS_SET_DATE, options.getDefaultHeaders())
              .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
              .setServerOption(UndertowOptions.DECODE_URL, false)
              /** Worker: */
              .setIoThreads(options.getIoThreads())
              .setWorkerOption(Options.WORKER_NAME, "worker")
              .setWorkerThreads(options.getWorkerThreads())
              .setHandler(handler);

      if (!options.isHttpsOnly()) {
        builder.addHttpListener(options.getPort(), options.getHost());
      }

      // HTTP @
      builder.setServerOption(ENABLE_HTTP2, options.isHttp2() == Boolean.TRUE);

      SSLContext sslContext = options.getSSLContext(application.getEnvironment().getClassLoader());
      if (sslContext != null) {
        builder.addHttpsListener(options.getSecurePort(), options.getHost(), sslContext);
        SslOptions ssl = options.getSsl();
        builder.setSocketOption(Options.SSL_ENABLED_PROTOCOLS, Sequence.of(ssl.getProtocol()));
        Optional.ofNullable(options.getSsl())
            .map(SslOptions::getClientAuth)
            .map(this::toSslClientAuthMode)
            .ifPresent(
                clientAuth -> builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, clientAuth));
      } else if (options.isHttpsOnly()) {
        throw new StartupException("Server configured for httpsOnly, but ssl options not set");
      }

      server = builder.build();
      server.start();
      // NOT IDEAL, but we need to fire onStart after server.start to get access to Worker
      fireStart(applications, server.getWorker());

      fireReady(Collections.singletonList(application));

      return this;
    } catch (RuntimeException x) {
      Throwable sourceException = x;
      Throwable cause = Optional.ofNullable(x.getCause()).orElse(x);
      if (Server.isAddressInUse(cause)) {
        sourceException = new BindException("Address already in use: " + options.getPort());
      }
      throw SneakyThrows.propagate(sourceException);
    }
  }

  private SslClientAuthMode toSslClientAuthMode(SslOptions.ClientAuth clientAuth) {
    switch (clientAuth) {
      case REQUESTED:
        return SslClientAuthMode.REQUESTED;
      case REQUIRED:
        return SslClientAuthMode.REQUIRED;
      default:
        return SslClientAuthMode.NOT_REQUESTED;
    }
  }

  @NonNull @Override
  public synchronized Server stop() {
    try {
      fireStop(applications);
      applications.clear();
      applications = null;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    } finally {
      shutdownServer();
    }
    return this;
  }

  private void shutdownServer() {
    if (server != null) {
      try {
        server.stop();
      } finally {
        server = null;
      }
    }
  }
}
