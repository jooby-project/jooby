/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCallHandler;
import io.grpc.ServerServiceDefinition;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.ServerOptions;
import io.jooby.ServiceRegistry;
import io.jooby.SneakyThrows;
import io.jooby.internal.grpc.DefaultGrpcProcessor;
import io.jooby.rpc.grpc.GrpcProcessor;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GrpcModuleTest {

  @Mock Jooby app;
  @Mock ServiceRegistry registry;
  @Mock ServerOptions serverOptions;

  @BeforeEach
  void setup() {
    // Generate a unique app name per test so the in-process gRPC servers never collide
    lenient().when(app.getName()).thenReturn(UUID.randomUUID().toString());
    lenient().when(app.getServices()).thenReturn(registry);

    // Default server option mocks
    lenient().when(app.getServerOptions()).thenReturn(serverOptions);
    lenient().when(serverOptions.getMaxRequestSize()).thenReturn(10485760); // 10MB
  }

  @Test
  void testModuleInstallation_InstancesAndNoCustomizers() throws Exception {
    BindableService instanceService =
        createMockService(BindableService.class, "pkg.InstanceService", "DoWork");

    // Test instances constructor
    GrpcModule module = new GrpcModule(instanceService);

    module.install(app);

    // 1. Verify SPI Processor registration
    verify(registry).put(eq(GrpcProcessor.class), any(DefaultGrpcProcessor.class));

    // 2. Verify fallback route registration for the instance service
    ArgumentCaptor<Route.Handler> routeCaptor = ArgumentCaptor.forClass(Route.Handler.class);
    verify(app).post(eq("/pkg.InstanceService/DoWork"), routeCaptor.capture());

    // Execute the fallback route handler to cover the IllegalStateException throw
    Context ctx = mock(Context.class);
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> {
              routeCaptor.getValue().apply(ctx);
            });
    assertTrue(
        ex.getMessage()
            .contains("reached the standard HTTP router for: /pkg.InstanceService/DoWork"));

    // 3. Capture and execute the onStarting hook (simulating server start)
    ArgumentCaptor<SneakyThrows.Runnable> onStartingCaptor =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    verify(app).onStarting(onStartingCaptor.capture());

    // Running this covers the builder logic with null customizers, and builds/starts the
    // channel/server
    onStartingCaptor.getValue().run();

    // 4. Capture and execute the onStop hooks (simulating server shutdown)
    ArgumentCaptor<AutoCloseable> onStopCaptor = ArgumentCaptor.forClass(AutoCloseable.class);
    verify(app, times(2)).onStop(onStopCaptor.capture());

    // Running these ensures shutdownNow() executes without exceptions
    for (var stopHook : onStopCaptor.getAllValues()) {
      stopHook.close();
    }
  }

  @Test
  void testModuleInstallation_ClassesAndCustomizers() throws Exception {
    BindableService diService1 =
        createMockService(BindableService.class, "pkg.DIService1", "ActionOne");
    DummyService diService2 = createMockService(DummyService.class, "pkg.DIService2", "ActionTwo");

    // Setup DI resolution mock (No casting required now)
    lenient().when(app.require(BindableService.class)).thenReturn(diService1);
    lenient().when(app.require(DummyService.class)).thenReturn(diService2);

    AtomicBoolean serverCustomizerRan = new AtomicBoolean(false);
    AtomicBoolean channelCustomizerRan = new AtomicBoolean(false);

    // Test Class constructor, the `bind()` chained method, and customizers
    GrpcModule module =
        new GrpcModule(BindableService.class)
            .bind(DummyService.class)
            .withServer(
                builder -> {
                  serverCustomizerRan.set(true);
                })
            .withChannel(
                builder -> {
                  channelCustomizerRan.set(true);
                });

    module.install(app);

    // Capture and run the onStarting hook (which resolves DI and runs customizers)
    ArgumentCaptor<SneakyThrows.Runnable> onStartingCaptor =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    verify(app).onStarting(onStartingCaptor.capture());

    // Execute
    onStartingCaptor.getValue().run();

    // Verify DI resolution occurred inside the starting hook
    verify(app).require(BindableService.class);
    verify(app).require(DummyService.class);

    // Verify the fallback routes were successfully mapped for the DI provisioned classes
    verify(app).post(eq("/pkg.DIService1/ActionOne"), any());
    verify(app).post(eq("/pkg.DIService2/ActionTwo"), any());

    // Verify customizers were executed
    assertTrue(serverCustomizerRan.get(), "Server customizer was not executed");
    assertTrue(channelCustomizerRan.get(), "Channel customizer was not executed");

    // Verify shutdown hooks were registered
    verify(app, times(2)).onStop(any(AutoCloseable.class));
  }

  // --- HELPER METHODS & CLASSES ---

  /**
   * Helper to dynamically construct a mock gRPC service with a valid MethodDescriptor. This
   * bypasses the need to compile actual proto files or create massive mock trees.
   *
   * @param type The specific class interface to mock (prevents ClassCastExceptions)
   */
  private <T extends BindableService> T createMockService(
      Class<T> type, String serviceName, String methodName) {
    T mockService = mock(type);

    MethodDescriptor<String, String> method =
        MethodDescriptor.<String, String>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
            .setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
            .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class))
            .build();

    ServerServiceDefinition def =
        ServerServiceDefinition.builder(serviceName)
            .addMethod(method, mock(ServerCallHandler.class))
            .build();

    lenient().when(mockService.bindService()).thenReturn(def);
    return mockService;
  }

  /** A dummy interface purely for differentiating DI resolution targets in the test. */
  private interface DummyService extends BindableService {}
}
