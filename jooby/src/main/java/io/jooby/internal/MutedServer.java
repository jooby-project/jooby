/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jooby.Jooby;
import io.jooby.LoggingService;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.output.OutputFactory;

public class MutedServer implements Server {
  private Server delegate;
  private List<String> mute;
  private LoggingService loggingService;

  private MutedServer(Server server, LoggingService loggingService, List<String> mute) {
    this.delegate = server;
    this.loggingService = loggingService;
    this.mute = mute;
  }

  @Override
  public OutputFactory getOutputFactory() {
    return delegate.getOutputFactory();
  }

  static Optional<LoggingService> loadLoggingService(ClassLoader classLoader) {
    return ServiceLoader.load(LoggingService.class, classLoader).findFirst();
  }

  public static Server mute(Server server, String... logger) {
    var loggingService = loadLoggingService(server.getClass().getClassLoader());

    if (loggingService.isEmpty()) {
      return server;
    }

    Server delegate = server instanceof MutedServer ? ((MutedServer) server).delegate : server;
    var existingMute =
        server instanceof MutedServer ? ((MutedServer) server).mute : delegate.getLoggerOff();

    Stream<String> newLoggers = logger == null ? Stream.empty() : Stream.of(logger);

    var mute =
        Stream.concat(existingMute.stream(), newLoggers).distinct().collect(Collectors.toList());

    return new MutedServer(delegate, loggingService.get(), mute);
  }

  public Server setOptions(ServerOptions options) {
    delegate.setOptions(options);
    return this;
  }

  public String getName() {
    return delegate.getName();
  }

  public ServerOptions getOptions() {
    return delegate.getOptions();
  }

  public Server start(Jooby... application) {
    loggingService.logOff(mute, () -> delegate.start(application));
    return this;
  }

  public List<String> getLoggerOff() {
    return mute;
  }

  public Server stop() {
    loggingService.logOff(mute, delegate::stop);
    return this;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
