/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.internal.vertx.VertxRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class VertxModuleTest {

  @Mock private Jooby application;
  @Mock private ServiceRegistry services;
  @Mock private Config config;

  private MockedStatic<Vertx> mockedVertx;
  private MockedStatic<VertxRegistry> mockedVertxRegistry;

  @BeforeEach
  void setUp() {
    mockedVertx = mockStatic(Vertx.class);
    mockedVertxRegistry = mockStatic(VertxRegistry.class);

    when(application.getServices()).thenReturn(services);
  }

  @AfterEach
  void tearDown() {
    mockedVertx.close();
    mockedVertxRegistry.close();
  }

  @Test
  void shouldThrowIfVertxAlreadyExists() {
    when(services.getOrNull(Vertx.class)).thenReturn(mock(Vertx.class));

    VertxModule module = new VertxModule();

    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> module.install(application));
    assertEquals("Vertx already exists.", thrown.getMessage());
  }

  @Test
  void shouldInstallWithEmptyOptionsWhenConfigDoesNotHaveVertxKey() throws Exception {
    when(services.getOrNull(Vertx.class)).thenReturn(null);
    when(application.getConfig()).thenReturn(config);
    when(config.hasPath("vertx")).thenReturn(false);

    Vertx vertx = simulateVertxCreationAndStop();

    VertxModule module = new VertxModule();
    module.install(application);

    // Verify VertxOptions fallback to default
    ArgumentCaptor<VertxOptions> optionsCaptor = ArgumentCaptor.forClass(VertxOptions.class);
    mockedVertx.verify(() -> Vertx.vertx(optionsCaptor.capture()));
    assertEquals(
        VertxOptions.DEFAULT_WORKER_POOL_SIZE, optionsCaptor.getValue().getWorkerPoolSize());

    mockedVertxRegistry.verify(() -> VertxRegistry.init(services, vertx));
  }

  @Test
  void shouldInstallWithOptionsExtractedFromConfig() throws Exception {
    when(services.getOrNull(Vertx.class)).thenReturn(null);
    when(application.getConfig()).thenReturn(config);
    when(config.hasPath("vertx")).thenReturn(true);

    ConfigObject configObj = mock(ConfigObject.class);
    when(configObj.unwrapped()).thenReturn(Map.of("workerPoolSize", 42));
    when(config.getObject("vertx")).thenReturn(configObj);

    simulateVertxCreationAndStop();

    VertxModule module = new VertxModule();
    module.install(application);

    // Verify VertxOptions mapped correctly from Config
    ArgumentCaptor<VertxOptions> optionsCaptor = ArgumentCaptor.forClass(VertxOptions.class);
    mockedVertx.verify(() -> Vertx.vertx(optionsCaptor.capture()));
    assertEquals(42, optionsCaptor.getValue().getWorkerPoolSize());
  }

  @Test
  void shouldInstallWithProvidedOptions() throws Exception {
    when(services.getOrNull(Vertx.class)).thenReturn(null);
    when(application.getConfig()).thenReturn(config);

    simulateVertxCreationAndStop();

    VertxOptions explicitOptions = new VertxOptions().setWorkerPoolSize(99);
    VertxModule module = new VertxModule(explicitOptions);
    module.install(application);

    ArgumentCaptor<VertxOptions> optionsCaptor = ArgumentCaptor.forClass(VertxOptions.class);
    mockedVertx.verify(() -> Vertx.vertx(optionsCaptor.capture()));
    assertEquals(99, optionsCaptor.getValue().getWorkerPoolSize());
  }

  @Test
  void shouldInstallWithProvidedVertxInstance() throws Exception {
    when(services.getOrNull(Vertx.class)).thenReturn(null);
    when(application.getConfig()).thenReturn(config);
    when(config.hasPath("vertx")).thenReturn(false);

    Vertx providedVertx = mock(Vertx.class);
    Future closeFuture = mock(Future.class);
    when(providedVertx.close()).thenReturn(closeFuture);
    interceptJoobyOnStopExecution();

    VertxModule module = new VertxModule(providedVertx);
    module.install(application);

    // Verify we didn't spin up a new instance
    mockedVertx.verifyNoInteractions();
    mockedVertxRegistry.verify(() -> VertxRegistry.init(services, providedVertx));

    verify(providedVertx).close();
    verify(closeFuture).await();
  }

  @Test
  void shouldInstallWithProvidedFunctionFactory() throws Exception {
    when(services.getOrNull(Vertx.class)).thenReturn(null);
    when(application.getConfig()).thenReturn(config);

    Vertx generatedVertx = mock(Vertx.class);
    Future<Vertx> factoryFuture = mock(Future.class);
    when(factoryFuture.await()).thenReturn(generatedVertx);

    Future closeFuture = mock(Future.class);
    when(generatedVertx.close()).thenReturn(closeFuture);
    interceptJoobyOnStopExecution();

    Function<VertxOptions, Future<Vertx>> factory = ops -> factoryFuture;

    VertxModule module = new VertxModule(factory);
    module.install(application);

    mockedVertxRegistry.verify(() -> VertxRegistry.init(services, generatedVertx));
  }

  @Test
  void shouldInstallWithProvidedSupplier() throws Exception {
    when(services.getOrNull(Vertx.class)).thenReturn(null);
    when(application.getConfig()).thenReturn(config);

    Vertx generatedVertx = mock(Vertx.class);
    Future<Vertx> factoryFuture = mock(Future.class);
    when(factoryFuture.await()).thenReturn(generatedVertx);

    Future closeFuture = mock(Future.class);
    when(generatedVertx.close()).thenReturn(closeFuture);
    interceptJoobyOnStopExecution();

    Supplier<Future<Vertx>> supplier = () -> factoryFuture;

    VertxModule module = new VertxModule(supplier);
    module.install(application);

    mockedVertxRegistry.verify(() -> VertxRegistry.init(services, generatedVertx));
  }

  /**
   * Helper that mocks Vertx creation, and ensures the Jooby onStop closure is executed immediately
   * so we can verify the close.await() chain in the same test pass.
   */
  private Vertx simulateVertxCreationAndStop() {
    Vertx vertx = mock(Vertx.class);
    mockedVertx.when(() -> Vertx.vertx(any(VertxOptions.class))).thenReturn(vertx);

    Future closeFuture = mock(Future.class);
    when(vertx.close()).thenReturn(closeFuture);

    interceptJoobyOnStopExecution();
    return vertx;
  }

  private void interceptJoobyOnStopExecution() {
    // Intercept the closure registered on application.onStop and run it immediately
    doAnswer(
            invocation -> {
              AutoCloseable task = invocation.getArgument(0);
              task.close();
              return application;
            })
        .when(application)
        .onStop(any());
  }
}
