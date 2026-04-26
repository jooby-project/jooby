/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import com.typesafe.config.Config;

public class StartupSummaryTest {

  private Jooby app;
  private Server server;
  private Logger logger;
  private Environment env;
  private Config config;
  private Router router;

  @BeforeEach
  void setUp() {
    app = mock(Jooby.class);
    server = mock(Server.class);
    logger = mock(Logger.class);
    env = mock(Environment.class);
    config = mock(Config.class);
    router = mock(Router.class);

    // Common standard mocks
    when(app.getLog()).thenReturn(logger);
    when(app.getEnvironment()).thenReturn(env);
    when(app.getConfig()).thenReturn(config);
    when(app.getName()).thenReturn("TestApp");
    when(app.getRouter()).thenReturn(router);
    when(app.getContextPath()).thenReturn("/api");
  }

  @Test
  void shouldCreateCorrectInstanceFromString() {
    assertEquals(StartupSummary.VERBOSE, StartupSummary.create("VERBOSE"));
    assertEquals(StartupSummary.VERBOSE, StartupSummary.create("verbose"));
    assertEquals(StartupSummary.NONE, StartupSummary.create("none"));
    assertEquals(StartupSummary.ROUTES, StartupSummary.create("routes"));

    // Fallback cases
    assertEquals(StartupSummary.DEFAULT, StartupSummary.create("default"));
    assertEquals(StartupSummary.DEFAULT, StartupSummary.create("unknown_value"));
  }

  @Test
  void noneShouldDoNothing() {
    StartupSummary.NONE.log(app, server);

    // Verify absolutely no interactions occurred with the application or server
    verifyNoInteractions(app);
    verifyNoInteractions(server);
  }

  @Test
  void defaultShouldLogSingleEnvironment() {
    when(env.getActiveNames()).thenReturn(Collections.singletonList("dev"));

    StartupSummary.DEFAULT.log(app, server);

    verify(logger).info("{} ({}) started", "TestApp", "dev");
  }

  @Test
  void defaultShouldLogMultipleEnvironments() {
    when(env.getActiveNames()).thenReturn(Arrays.asList("dev", "test"));

    StartupSummary.DEFAULT.log(app, server);

    verify(logger).info("{} ({}) started", "TestApp", "[dev, test]");
  }

  @Test
  void verboseShouldLogAllDetailsIncludingLogFile() {
    ServerOptions options = new ServerOptions();
    Path tmpDir = Paths.get("/tmp/jooby");

    when(config.getString(AvailableSettings.PID)).thenReturn("9999");
    when(server.getOptions()).thenReturn(options);
    when(app.getExecutionMode()).thenReturn(ExecutionMode.DEFAULT);
    when(config.getString("user.dir")).thenReturn("/opt/app");
    when(app.getTmpdir()).thenReturn(tmpDir);

    // Test branch where LOG_FILE exists
    when(config.hasPath(AvailableSettings.LOG_FILE)).thenReturn(true);
    when(config.getString(AvailableSettings.LOG_FILE)).thenReturn("/var/log/app.log");

    StartupSummary.VERBOSE.log(app, server);

    verify(logger).info("{} started with:", "TestApp");
    verify(logger).info("    PID: {}", "9999");
    verify(logger).info("    {}", options);
    verify(logger).info("    execution mode: {}", "default");
    verify(logger).info("    environment: {}", env);
    verify(logger).info("    app dir: {}", "/opt/app");
    verify(logger).info("    tmp dir: {}", tmpDir);
    verify(logger).info("    log file: {}", "/var/log/app.log");
  }

  @Test
  void verboseShouldSkipLogFileIfMissing() {
    ServerOptions options = new ServerOptions();
    when(config.getString(AvailableSettings.PID)).thenReturn("9999");
    when(server.getOptions()).thenReturn(options);
    when(app.getExecutionMode()).thenReturn(ExecutionMode.DEFAULT);
    when(config.getString("user.dir")).thenReturn("/opt/app");

    // Test branch where LOG_FILE does NOT exist
    when(config.hasPath(AvailableSettings.LOG_FILE)).thenReturn(false);

    StartupSummary.VERBOSE.log(app, server);

    verify(logger, never()).info(eq("    log file: {}"), anyString());
  }

  @Test
  void routesShouldLogHttpOnly() {
    ServerOptions options = new ServerOptions().setHost("0.0.0.0").setPort(8080);
    when(server.getOptions()).thenReturn(options);

    StartupSummary.ROUTES.log(app, server);

    ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);

    verify(logger)
        .info(eq("routes: \n\n{}\n\nlistening on:\n  http://{}:{}{}\n"), argsCaptor.capture());

    Object[] capturedArgs = argsCaptor.getValue();
    assertEquals(4, capturedArgs.length);
    assertEquals(router, capturedArgs[0]);
    assertEquals("localhost", capturedArgs[1]); // Replaces 0.0.0.0
    assertEquals(8080, capturedArgs[2]);
    assertEquals("/api", capturedArgs[3]);
  }

  @Test
  void routesShouldLogHttpsOnly() {
    ServerOptions options =
        new ServerOptions()
            .setHost("localhost")
            .setSecurePort(8443)
            .setHttpsOnly(true); // Triggers the HTTPS only branch

    when(server.getOptions()).thenReturn(options);

    StartupSummary.ROUTES.log(app, server);

    ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);

    verify(logger)
        .info(eq("routes: \n\n{}\n\nlistening on:\n  https://{}:{}{}\n"), argsCaptor.capture());

    Object[] capturedArgs = argsCaptor.getValue();
    assertEquals(4, capturedArgs.length);
    assertEquals(router, capturedArgs[0]);
    assertEquals("localhost", capturedArgs[1]);
    assertEquals(8443, capturedArgs[2]);
    assertEquals("/api", capturedArgs[3]);
  }

  @Test
  void routesShouldLogBothHttpAndHttps() {
    ServerOptions options =
        new ServerOptions()
            .setHost("myapp.com")
            .setPort(80)
            .setSecurePort(443)
            .setHttp2(true); // Triggers both HTTP and HTTPS appending

    when(server.getOptions()).thenReturn(options);

    StartupSummary.ROUTES.log(app, server);

    ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);

    verify(logger)
        .info(
            eq("routes: \n\n{}\n\nlistening on:\n  http://{}:{}{}\n  https://{}:{}{}\n"),
            argsCaptor.capture());

    Object[] capturedArgs = argsCaptor.getValue();
    assertEquals(7, capturedArgs.length);
    assertEquals(router, capturedArgs[0]);

    // HTTP Args
    assertEquals("myapp.com", capturedArgs[1]);
    assertEquals(80, capturedArgs[2]);
    assertEquals("/api", capturedArgs[3]);

    // HTTPS Args
    assertEquals("myapp.com", capturedArgs[4]);
    assertEquals(443, capturedArgs[5]);
    assertEquals("/api", capturedArgs[6]);
  }
}
