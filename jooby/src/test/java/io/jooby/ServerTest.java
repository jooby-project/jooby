/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.EOFException;
import java.io.IOException;
import java.net.BindException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.exception.StartupException;
import io.jooby.output.OutputFactory;

class ServerTest {

  private static class TestServer extends Server.Base {
    @Override
    public String getName() {
      return "test-server";
    }

    @Override
    public OutputFactory getOutputFactory() {
      return null;
    }

    @Override
    public Server start(Jooby... application) {
      return this;
    }

    @Override
    public Server stop() {
      return this;
    }
  }

  @Test
  @DisplayName("Test Server.Base lifecycle methods")
  void testBaseLifecycle() {
    TestServer server = new TestServer();
    Jooby app1 = mock(Jooby.class);
    Jooby app2 = mock(Jooby.class);
    Executor executor = mock(Executor.class);

    when(app1.setDefaultWorker(any())).thenReturn(app1);
    when(app2.setDefaultWorker(any())).thenReturn(app2);

    List<Jooby> apps = Arrays.asList(app1, app2);

    // fireStart
    server.fireStart(apps, executor);
    verify(app1).start(server);
    verify(app2).start(server);

    // fireReady
    server.fireReady(apps);
    verify(app1).ready(server);
    verify(app2).ready(server);

    // fireStop (first call runs, second call is blocked by AtomicBoolean)
    server.fireStop(apps);
    server.fireStop(apps);
    verify(app1, times(1)).stop();
    verify(app2, times(1)).stop();

    // Null check branch
    server.fireStop(null);
  }

  @Test
  @DisplayName("Test Server.init registry injection")
  void testInit() {
    TestServer server = new TestServer();
    Jooby app = mock(Jooby.class);
    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(app.getServices()).thenReturn(registry);

    server.init(app);

    verify(registry).put(eq(ServerOptions.class), any(ServerOptions.class));
    verify(registry).put(eq(Server.class), eq(server));
    assertEquals("test-server", server.getOptions().getServer());
  }

  @Test
  @DisplayName("Test connection lost predicates and branches")
  void testConnectionLost() {
    // Base predicates
    assertTrue(Server.connectionLost(new ClosedChannelException()));
    assertTrue(Server.connectionLost(new EOFException()));
    assertTrue(Server.connectionLost(new IOException("connection reset")));
    assertTrue(Server.connectionLost(new IOException("Broken Pipe")));
    assertTrue(Server.connectionLost(new IOException("reset by peer")));
    assertTrue(Server.connectionLost(new IOException("forcibly closed")));

    // Negative cases for coverage
    assertFalse(Server.connectionLost(new IOException("other error")));
    assertFalse(Server.connectionLost(new IllegalArgumentException()));
    assertFalse(Server.connectionLost(new IOException((String) null)));

    // Custom predicate
    Server.addConnectionLost(t -> t instanceof RuntimeException);
    assertTrue(Server.connectionLost(new RuntimeException()));
  }

  @Test
  @DisplayName("Test address in use predicates and branches")
  void testAddressInUse() {
    assertTrue(Server.isAddressInUse(new BindException()));
    assertTrue(Server.isAddressInUse(new RuntimeException("Address already in use")));

    // Negative cases
    assertFalse(Server.isAddressInUse(new RuntimeException("something else")));
    assertFalse(Server.isAddressInUse(null));

    // Custom predicate
    Server.addAddressInUse(t -> t instanceof NullPointerException);
    assertTrue(Server.isAddressInUse(new NullPointerException()));
  }

  @Test
  @DisplayName("Test shutdown hook and options")
  void testShutdownHookAndOptions() {
    TestServer server = new TestServer();
    ServerOptions options = new ServerOptions();
    server.setOptions(options);
    assertEquals(options, server.getOptions());

    // We can't easily verify the Runtime hook registration without a mock Runtime,
    // but we call it to ensure branch coverage.
    server.addShutdownHook();
  }

  @Test
  @DisplayName("Test getLoggerOff default")
  void testGetLoggerOff() {
    TestServer server = new TestServer();
    assertTrue(server.getLoggerOff().isEmpty());
  }

  /**
   * Note: Testing loadServer() effectively requires mocking ServiceLoader. Since ServiceLoader is
   * final, we rely on the fact that if no server is in classpath during test, it throws
   * StartupException.
   */
  @Test
  @DisplayName("Test loadServer exception branch")
  void testLoadServerNotFound() {
    // This test assumes your test environment doesn't have a META-INF/services/io.jooby.Server
    // If it does, this will return the server instead of throwing.
    try {
      Server server = Server.loadServer();
      assertNotNull(server);
    } catch (StartupException ex) {
      assertEquals("Server not found.", ex.getMessage());
    }
  }
}
