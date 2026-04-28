/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.jooby.*;
import io.jooby.output.OutputFactory;

public class MutedServerTest {

  @Test
  @DisplayName("Test early-exit muting logic, logger aggregation, and null vararg safety")
  void testMutedServer() {
    Server delegate = mock(Server.class);
    when(delegate.getLoggerOff()).thenReturn(Collections.singletonList("base.logger"));
    when(delegate.getName()).thenReturn("mock-server");
    when(delegate.toString()).thenReturn("MockServer");

    ServerOptions options = new ServerOptions();
    when(delegate.getOptions()).thenReturn(options);
    OutputFactory outFactory = mock(OutputFactory.class);
    when(delegate.getOutputFactory()).thenReturn(outFactory);

    LoggingService loggingService = mock(LoggingService.class);

    // Mock logOff to execute the lambda synchronously
    doAnswer(
            inv -> {
              Runnable action = inv.getArgument(1);
              action.run();
              return null;
            })
        .when(loggingService)
        .logOff(anyList(), any(SneakyThrows.Runnable.class));

    Jooby app = new Jooby();

    // MOCK MutedServer.class INSTEAD OF ServiceLoader
    try (MockedStatic<MutedServer> mockedServer =
        mockStatic(MutedServer.class, CALLS_REAL_METHODS)) {
      // Force our extracted method to return the mocked logging service
      mockedServer
          .when(() -> MutedServer.loadLoggingService(any()))
          .thenReturn(Optional.of(loggingService));

      // 1. Create MutedServer
      Server muted1 = MutedServer.mute(delegate, "new.logger");
      assertTrue(muted1 instanceof MutedServer, "Should be wrapped in MutedServer");

      // 2. Test Chaining MutedServer & Null Varargs Safety
      Server muted2 = MutedServer.mute(muted1, (String[]) null);
      muted2 = MutedServer.mute(muted2, "another.logger");
      assertTrue(muted2 instanceof MutedServer);

      // Verify simple delegates
      assertEquals("mock-server", muted2.getName());
      assertSame(options, muted2.getOptions());
      assertSame(outFactory, muted2.getOutputFactory());
      assertEquals("MockServer", muted2.toString());

      // Verify merged loggers
      List<String> combinedMute = muted2.getLoggerOff();
      assertTrue(combinedMute.contains("base.logger"));
      assertTrue(combinedMute.contains("new.logger"));
      assertTrue(combinedMute.contains("another.logger"));

      // 3. Verify Fluent Fixes
      ServerOptions newOpts = new ServerOptions();
      assertSame(muted2, muted2.setOptions(newOpts));
      verify(delegate).setOptions(newOpts);

      assertSame(muted2, muted2.start(app));
      verify(delegate).start(app);
      verify(loggingService).logOff(eq(combinedMute), any(SneakyThrows.Runnable.class));

      assertSame(muted2, muted2.stop());
      verify(delegate).stop();
    }
  }

  @Test
  @DisplayName("Test early exit when no LoggingService is present")
  void testMuteEarlyExitNoLoggingService() {
    Server delegate = mock(Server.class);

    // MOCK MutedServer.class
    try (MockedStatic<MutedServer> mockedServer =
        mockStatic(MutedServer.class, CALLS_REAL_METHODS)) {
      // Force it to return empty, triggering the if (loggingService.isEmpty()) branch
      mockedServer.when(() -> MutedServer.loadLoggingService(any())).thenReturn(Optional.empty());

      Server unmuted = MutedServer.mute(delegate, "some.logger");

      // Asserts that the original delegate is returned untouched
      assertSame(delegate, unmuted);
    }
  }
}
