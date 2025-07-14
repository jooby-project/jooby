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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Jooby;
import io.jooby.LoggingService;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.buffer.BufferedOutputFactory;

public class MutedServer implements Server {
  private Server delegate;

  private List<String> mute;

  private LoggingService loggingService;

  private MutedServer(Server server, LoggingService loggingService, List<String> mute) {
    this.delegate = server;
    this.loggingService = loggingService;
    this.mute = mute;
  }

  @NonNull @Override
  public BufferedOutputFactory getOutputFactory() {
    return delegate.getOutputFactory();
  }

  /**
   * Muted a server when need it.
   *
   * @param server Server to mute.
   * @return Muted server or same server.
   */
  public static Server mute(Server server, String... logger) {
    if (server instanceof MutedServer) {
      return server;
    }
    List<String> mute =
        Stream.concat(server.getLoggerOff().stream(), Stream.of(logger))
            .collect(Collectors.toList());

    Optional<LoggingService> loggingService =
        ServiceLoader.load(LoggingService.class, server.getClass().getClassLoader()).findFirst();
    return loggingService
        .filter(service -> !mute.isEmpty())
        .<Server>map(service -> new MutedServer(server, service, mute))
        .orElse(server);
  }

  @NonNull public Server setOptions(@NonNull ServerOptions options) {
    return delegate.setOptions(options);
  }

  @NonNull public String getName() {
    return delegate.getName();
  }

  @NonNull public ServerOptions getOptions() {
    return delegate.getOptions();
  }

  @NonNull public Server start(@NonNull Jooby... application) {
    loggingService.logOff(mute, () -> delegate.start(application));
    return delegate;
  }

  @NonNull public List<String> getLoggerOff() {
    return delegate.getLoggerOff();
  }

  @NonNull public Server stop() {
    loggingService.logOff(mute, delegate::stop);
    return delegate;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
