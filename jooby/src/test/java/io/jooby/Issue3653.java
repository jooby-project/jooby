/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.output.OutputFactory;

public class Issue3653 {

  private static class TestServer extends Server.Base {

    @NotNull @Override
    public OutputFactory getOutputFactory() {
      return null;
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
  public void shouldNotWarnWhenOptionsAreSetForFirstTime() {
    try (var factory = Mockito.mockStatic(LoggerFactory.class)) {
      var server = new TestServer();
      var mockLogger = Mockito.mock(Logger.class);
      factory.when(() -> LoggerFactory.getLogger(TestServer.class)).thenReturn(mockLogger);
      server.setOptions(new ServerOptions());
      Mockito.verify(mockLogger, Mockito.times(0)).warn(Mockito.isA(String.class));
    }
  }
}
