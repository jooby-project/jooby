/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.ServiceRegistry;
import io.jooby.internal.vertx.VertxRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class VertxServerTest {

  @Mock private Jooby app;
  @Mock private ServiceRegistry services;

  private MockedStatic<Vertx> mockedVertx;
  private MockedStatic<VertxRegistry> mockedRegistry;

  @BeforeEach
  void setup() {
    // Lenient prevents strict stubbing errors on tests that don't call app.getServices()
    lenient().when(app.getServices()).thenReturn(services);

    mockedVertx = mockStatic(Vertx.class);
    mockedRegistry = mockStatic(VertxRegistry.class);
  }

  @AfterEach
  void teardown() {
    mockedVertx.close();
    mockedRegistry.close();
  }

  @Test
  void shouldInitializeWithProvidedVertxOptions() {
    VertxOptions options = new VertxOptions();
    Vertx vertx = mock(Vertx.class);
    mockedVertx.when(() -> Vertx.vertx(options)).thenReturn(vertx);

    VertxServer server = new VertxServer(options);

    server.init(app);

    mockedRegistry.verify(() -> VertxRegistry.init(services, vertx));
  }

  @Test
  void shouldInitializeWithProvidedVertxInstance() {
    Vertx vertx = mock(Vertx.class);
    VertxServer server = new VertxServer(vertx);

    server.init(app);

    // Asserts we bypassed static creation entirely when an instance is provided
    mockedVertx.verifyNoInteractions();
    mockedRegistry.verify(() -> VertxRegistry.init(services, vertx));
  }

  @Test
  void shouldLazilyInitializeVertxFromJoobyServerOptions() {
    Vertx vertx = mock(Vertx.class);
    mockedVertx.when(() -> Vertx.vertx(any(VertxOptions.class))).thenReturn(vertx);

    VertxServer server = new VertxServer();

    // Provide standard Jooby ServerOptions to test mapping
    ServerOptions serverOptions = new ServerOptions().setIoThreads(4).setWorkerThreads(16);
    server.setOptions(serverOptions);

    server.init(app);

    ArgumentCaptor<VertxOptions> optionsCaptor = ArgumentCaptor.forClass(VertxOptions.class);
    mockedVertx.verify(() -> Vertx.vertx(optionsCaptor.capture()));

    VertxOptions capturedOpts = optionsCaptor.getValue();
    assertEquals(true, capturedOpts.getPreferNativeTransport());
    assertEquals(4, capturedOpts.getEventLoopPoolSize());
    assertEquals(16, capturedOpts.getWorkerPoolSize());

    mockedRegistry.verify(() -> VertxRegistry.init(services, vertx));
  }

  @Test
  void shouldReturnCorrectServerName() {
    VertxServer server = new VertxServer();

    assertEquals("vertx", server.getName());
    assertEquals("vertx", System.getProperty("__server_.name"));
  }

  @Test
  void shouldCreateEventLoopGroup() {
    Vertx vertx = mock(Vertx.class);
    VertxServer server = new VertxServer(vertx);

    assertNotNull(server.createEventLoopGroup());
  }

  @Test
  void stopShouldCloseUnderlyingVertxInstance() {
    Vertx vertx = mock(Vertx.class);
    Future closeFuture = mock(Future.class);
    lenient().when(vertx.close()).thenReturn(closeFuture);

    VertxServer server = new VertxServer(vertx);
    server.stop();

    verify(vertx).close();
    verify(closeFuture).await();
  }

  @Test
  void stopShouldNotThrowIfVertxIsNull() {
    VertxServer server = new VertxServer();

    // server.init(...) was not called, so vertx is still null
    server.stop();

    // Success condition: No NPE thrown
  }
}
