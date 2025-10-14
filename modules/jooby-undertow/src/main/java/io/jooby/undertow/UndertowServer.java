/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.undertow;

import static io.undertow.UndertowOptions.ENABLE_HTTP2;

import java.net.BindException;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.xnio.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.*;
import io.jooby.exception.StartupException;
import io.jooby.internal.undertow.UndertowHandler;
import io.jooby.internal.undertow.UndertowWebSocket;
import io.jooby.output.OutputFactory;
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

  private static final String NAME = "undertow";

  static {
    // Default values
    System.setProperty("__server_.name", NAME);
  }

  private static final int BACKLOG = 8192;

  private static final int _100 = 100;

  private static final int _10 = 10;

  private Undertow server;

  private List<Jooby> applications;

  private XnioWorker worker;

  private OutputFactory outputFactory;

  public UndertowServer(@NonNull ServerOptions options) {
    setOptions(options);
  }

  public UndertowServer() {}

  @Override
  public OutputFactory getOutputFactory() {
    if (outputFactory == null) {
      outputFactory = OutputFactory.create(getOptions().getOutput());
    }
    return outputFactory;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Server start(@NonNull Jooby... application) {
    // force options to be non-null
    var options = getOptions();
    var portInUse = options.getPort();
    try {
      this.applications = List.of(application);

      addShutdownHook();

      HttpHandler handler =
          new UndertowHandler(
              Context.Selector.create(applications),
              getOptions().getOutput().getSize(),
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
      var xnio = Xnio.getInstance(Undertow.class.getClassLoader());
      this.worker =
          xnio.createWorker(
              OptionMap.builder()
                  .set(Options.WORKER_IO_THREADS, options.getIoThreads())
                  .set(Options.CONNECTION_HIGH_WATER, 1000000)
                  .set(Options.CONNECTION_LOW_WATER, 1000000)
                  .set(Options.WORKER_TASK_CORE_THREADS, options.getWorkerThreads())
                  .set(Options.WORKER_TASK_MAX_THREADS, options.getWorkerThreads())
                  .set(Options.TCP_NODELAY, true)
                  .set(Options.CORK, true)
                  .addAll(OptionMap.create(Options.WORKER_NAME, "worker"))
                  .getMap());

      var builder =
          Undertow.builder()
              .setBufferSize(options.getOutput().getSize())
              .setDirectBuffers(options.getOutput().isDirectBuffers())
              /* Socket : */
              .setSocketOption(Options.BACKLOG, BACKLOG)
              /* Server: */
              // HTTP/1.1 is keep-alive by default, turn this option off
              .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
              .setServerOption(UndertowOptions.MAX_HEADER_SIZE, options.getMaxHeaderSize())
              .setServerOption(UndertowOptions.MAX_PARAMETERS, options.getMaxFormFields())
              // .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, (long)
              // options.getMaxRequestSize())
              .setServerOption(UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, true)
              .setServerOption(UndertowOptions.ALWAYS_SET_DATE, options.getDefaultHeaders())
              .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
              .setServerOption(UndertowOptions.DECODE_URL, false)
              /* Worker: */
              .setWorker(worker)
              .setHandler(handler);

      if (!options.isHttpsOnly()) {
        builder.addHttpListener(options.getPort(), options.getHost());
      }

      // HTTP @
      builder.setServerOption(ENABLE_HTTP2, options.isHttp2() == Boolean.TRUE);
      var classLoader = this.applications.get(0).getClassLoader();
      SSLContext sslContext = options.getSSLContext(classLoader);
      if (sslContext != null) {
        portInUse = options.getSecurePort();
        builder.addHttpsListener(portInUse, options.getHost(), sslContext);
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
      fireStart(applications, worker);
      server = builder.build();
      server.start();

      fireReady(applications);

      return this;
    } catch (Exception x) {
      Throwable sourceException = x;
      Throwable cause = Optional.ofNullable(x.getCause()).orElse(x);
      if (Server.isAddressInUse(cause)) {
        sourceException = new BindException("Address already in use: " + portInUse);
      }
      throw SneakyThrows.propagate(sourceException);
    }
  }

  @Override
  public List<String> getLoggerOff() {
    return List.of("org.xnio", "io.undertow", "org.jboss.threads");
  }

  private SslClientAuthMode toSslClientAuthMode(SslOptions.ClientAuth clientAuth) {
    return switch (clientAuth) {
      case REQUESTED -> SslClientAuthMode.REQUESTED;
      case REQUIRED -> SslClientAuthMode.REQUIRED;
      default -> SslClientAuthMode.NOT_REQUESTED;
    };
  }

  @Override
  public synchronized Server stop() {
    try {
      fireStop(applications);
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    } finally {
      // only for jooby build where close events may take longer.
      UndertowWebSocket.all.clear();
      shutdownServer();
    }
    return this;
  }

  private void shutdownServer() {
    try {
      if (server != null) {
        server.stop();
      }
    } finally {
      shutdownWorker();
      server = null;
    }
  }

  private void shutdownWorker() {
    /*
     * Only shutdown the worker if it was created during start()
     */
    if (worker != null) {
      worker.shutdown();
      try {
        worker.awaitTermination();
      } catch (InterruptedException e) {
        worker.shutdownNow();
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      } finally {
        worker = null;
      }
    }
  }
}
