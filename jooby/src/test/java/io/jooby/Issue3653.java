/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.output.OutputFactory;

public class Issue3653 {

  private static final ServerOptions defaultOptions = new ServerOptions();

  private static class TestServer extends Server.Base {

    @NotNull @Override
    public OutputFactory getOutputFactory() {
      return null;
    }

    @Override
    protected ServerOptions defaultOptions() {
      return defaultOptions;
    }

    @NotNull @Override
    public String getName() {
      return "Test";
    }

    @NotNull @Override
    public Server start(@NotNull Jooby... application) {
      return this;
    }

    @NotNull @Override
    public Server stop() {
      return this;
    }
  }

  @Test
  public void shouldNotWarnWhenDefaultOptionsAreSet() {
    try (var factory = Mockito.mockStatic(LoggerFactory.class)) {
      var server = new TestServer();
      var mockLogger = Mockito.mock(Logger.class);
      factory.when(() -> LoggerFactory.getLogger(TestServer.class)).thenReturn(mockLogger);
      assertEquals(defaultOptions, server.getOptions());
      server.setOptions(new ServerOptions());
      Mockito.verify(mockLogger, Mockito.times(0)).warn(Mockito.isA(String.class));
    }
  }

  @Test
  public void shouldNotWarnWhenOptionsAreSetForFirstTime() {
    try (var factory = Mockito.mockStatic(LoggerFactory.class)) {
      var server = new TestServer();
      var mockLogger = Mockito.mock(Logger.class);
      factory.when(() -> LoggerFactory.getLogger(TestServer.class)).thenReturn(mockLogger);
      server.setOptions(new ServerOptions());
      Mockito.verify(mockLogger, Mockito.times(0)).warn(Mockito.isA(String.class));
    }
  }

  @Test
  public void shouldWarnWhenOptionsAreSetMultipleTimes() {
    try (var factory = Mockito.mockStatic(LoggerFactory.class)) {
      var server = new TestServer();
      var mockLogger = Mockito.mock(Logger.class);
      factory.when(() -> LoggerFactory.getLogger(TestServer.class)).thenReturn(mockLogger);
      // first OK
      server.setOptions(new ServerOptions());
      Mockito.verify(mockLogger, Mockito.times(0)).warn(Mockito.isA(String.class));
      // Second warn
      server.setOptions(new ServerOptions());
      Mockito.verify(mockLogger, Mockito.times(1))
          .warn(
              "Server options must be called once. To turn off this warning set the: `{}` system"
                  + " property to `false`",
              AvailableSettings.SERVER_OPTIONS_WARN);
    }
  }

  @Test
  public void shouldNotWarnWhenOptionsAreSetMultipleTimesWhenOptionIsOff() {
    try (var factory = Mockito.mockStatic(LoggerFactory.class)) {
      System.setProperty(AvailableSettings.SERVER_OPTIONS_WARN, "false");
      var server = new TestServer();
      var mockLogger = Mockito.mock(Logger.class);
      factory.when(() -> LoggerFactory.getLogger(TestServer.class)).thenReturn(mockLogger);
      // first OK
      server.setOptions(new ServerOptions());
      Mockito.verify(mockLogger, Mockito.times(0)).warn(Mockito.isA(String.class));
      // Second OK
      server.setOptions(new ServerOptions());
      Mockito.verify(mockLogger, Mockito.times(0)).warn(Mockito.isA(String.class));
      System.getProperties().remove(AvailableSettings.SERVER_OPTIONS_WARN);
    }
  }
}
