/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.typesafe.config.Config;
import io.jooby.exception.StartupException;
import io.jooby.internal.MutedServer;

class JoobyRunnerTest {

  private MockedStatic<Jooby> runnerMock;
  private MockedStatic<ServerOptions> optionsMock;
  private MockedStatic<MutedServer> mutedMock;

  private Server server;
  private ServerOptions serverOptions;
  private ExecutionMode executionMode;
  private List<Supplier<Jooby>> providers;

  @BeforeEach
  void setUp() {
    // Mock static methods to isolate runApp
    runnerMock = mockStatic(Jooby.class, Mockito.CALLS_REAL_METHODS);
    optionsMock = mockStatic(ServerOptions.class);
    mutedMock = mockStatic(MutedServer.class);

    // Standard setup for method arguments
    server = mock(Server.class);
    executionMode = ExecutionMode.DEFAULT; // Assuming there is a DEFAULT, or mock it
    providers = new ArrayList<>();

    serverOptions = new ServerOptions(true);
    when(server.getOptions()).thenReturn(serverOptions);

    // Mock parseArguments to return an empty map by default to avoid polluting System properties
    runnerMock.when(() -> Jooby.parseArguments(any())).thenReturn(Collections.emptyMap());
  }

  @AfterEach
  void tearDown() {
    runnerMock.close();
    optionsMock.close();
    mutedMock.close();
  }

  @Test
  @DisplayName("Test Happy Path: Multiple apps, Muted Server, Defaults True")
  void testRunApp_MultipleApps_MutedServer_DefaultsTrue() {
    String[] args = new String[] {"arg1"};

    // Setup MutedServer branch (loggerOff is NOT empty)
    when(server.getLoggerOff()).thenReturn(List.of("SomeLogger"));
    Server mutedServer = mock(Server.class);
    mutedMock.when(() -> MutedServer.mute(server)).thenReturn(mutedServer);

    // Setup multiple apps to cover the `if (appServerOptions == null)` branches
    Supplier<Jooby> provider1 = mock(Supplier.class);
    Supplier<Jooby> provider2 = mock(Supplier.class);
    providers.add(provider1);
    providers.add(provider2);

    Jooby app1 = mock(Jooby.class);
    Jooby app2 = mock(Jooby.class);
    Config config1 = mock(Config.class);

    when(app1.getConfig()).thenReturn(config1);

    runnerMock.when(() -> Jooby.createApp(server, executionMode, provider1)).thenReturn(app1);
    runnerMock.when(() -> Jooby.createApp(server, executionMode, provider2)).thenReturn(app2);

    ServerOptions appOptions = new ServerOptions();
    optionsMock.when(() -> ServerOptions.from(config1)).thenReturn(Optional.of(appOptions));

    // Execution
    Jooby.runApp(args, server, executionMode, providers);

    // Verification
    // Defaults was true, so server.setOptions should be called with appOptions
    verify(server).setOptions(appOptions);
    verify(mutedServer).start(new Jooby[] {app1, app2});
  }

  @Test
  @DisplayName("Test Happy Path: Single app, Normal Server, Defaults False")
  void testRunApp_SingleApp_NormalServer_DefaultsFalse() {
    String[] args = new String[0];

    // Setup Normal Server branch (loggerOff IS empty)
    when(server.getLoggerOff()).thenReturn(Collections.emptyList());

    // Set defaults to false to skip the override branch
    serverOptions = new ServerOptions(false);
    when(server.getOptions()).thenReturn(serverOptions);

    Supplier<Jooby> provider1 = mock(Supplier.class);
    providers.add(provider1);

    Jooby app1 = mock(Jooby.class);
    Config config1 = mock(Config.class);
    when(app1.getConfig()).thenReturn(config1);
    runnerMock.when(() -> Jooby.createApp(server, executionMode, provider1)).thenReturn(app1);

    optionsMock.when(() -> ServerOptions.from(config1)).thenReturn(Optional.empty());

    // Execution
    Jooby.runApp(args, server, executionMode, providers);

    // Verification
    verify(server, never()).setOptions(any()); // Because defaults == false
    verify(server).start(new Jooby[] {app1});
  }

  @DisplayName("Test Exception: StartupException thrown, stop throws ignored exception")
  void testRunApp_StartupException_StopThrows() {
    String[] args = new String[0];
    when(server.getLoggerOff()).thenReturn(Collections.emptyList());

    Supplier<Jooby> provider1 = mock(Supplier.class);
    providers.add(provider1);

    StartupException expectedException = new StartupException("Simulated startup failure");

    // Force createApp to throw an exception
    runnerMock
        .when(() -> Jooby.createApp(server, executionMode, provider1))
        .thenThrow(expectedException);

    // Force targetServer.stop() to throw an exception to cover the `ignored` catch block
    doThrow(new RuntimeException("Stop failed")).when(server).stop();

    // Execution & Verification
    StartupException thrown =
        assertThrows(
            StartupException.class, () -> Jooby.runApp(args, server, executionMode, providers));

    assertEquals(expectedException, thrown);
    verify(server).stop(); // Ensure stop was attempted
  }

  @Test
  @DisplayName("Test Exception: Generic exception thrown, stop succeeds, wraps in StartupException")
  void testRunApp_GenericException_StopSucceeds() {
    String[] args = new String[0];
    when(server.getLoggerOff()).thenReturn(Collections.emptyList());

    Supplier<Jooby> provider1 = mock(Supplier.class);
    providers.add(provider1);

    Jooby app1 = mock(Jooby.class);
    runnerMock.when(() -> Jooby.createApp(server, executionMode, provider1)).thenReturn(app1);
    optionsMock.when(() -> ServerOptions.from(any())).thenReturn(Optional.empty());

    RuntimeException genericException = new RuntimeException("Something bad happened");

    // Force start() to throw a generic exception
    doThrow(genericException).when(server).start(any());

    // Execution & Verification
    StartupException thrown =
        assertThrows(
            StartupException.class, () -> Jooby.runApp(args, server, executionMode, providers));

    assertTrue(thrown.getMessage().contains("Application initialization resulted in exception"));
    assertEquals(genericException, thrown.getCause());

    // Verify stop succeeded gracefully
    verify(server).stop();
  }
}
