/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.util.*;

import org.slf4j.bridge.SLF4JBridgeHandler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.jooby.*;
import io.jooby.internal.grpc.DefaultGrpcProcessor;
import io.jooby.rpc.grpc.GrpcProcessor;

/**
 * Native gRPC extension for Jooby.
 *
 * <p>This module allows you to run strictly-typed gRPC services alongside standard Jooby HTTP
 * routes on the exact same port. It completely bypasses standard HTTP/1.1 pipelines in favor of a
 * highly optimized, reactive, native interceptor tailored for HTTP/2 multiplexing and trailing
 * headers.
 *
 * <h3>Usage</h3>
 *
 * <p>gRPC requires HTTP/2. Ensure your Jooby application is configured to use a supported server
 * with HTTP/2 enabled.
 *
 * <pre>{@code
 * import io.jooby.Jooby;
 * import io.jooby.ServerOptions;
 * import io.jooby.grpc.GrpcModule;
 * * public class App extends Jooby {
 * {
 * setServerOptions(new ServerOptions().setHttp2(true).setSecurePort(8443));
 * * // Install the extension and register your services
 * install(new GrpcModule(new GreeterService()));
 * }
 * }
 * }</pre>
 *
 * <h3>Dependency Injection</h3>
 *
 * <p>If your gRPC services require external dependencies (like repositories or configuration), you
 * can register the service classes instead of instances. The module will automatically provision
 * them using Jooby's DI registry (e.g., Guice, Spring) during the application startup phase.
 *
 * <pre>{@code
 * public class App extends Jooby {
 * {
 * install(new GuiceModule());
 * * // Pass the class reference. Guice will instantiate it!
 * install(new GrpcModule(GreeterService.class));
 * }
 * }
 * }</pre>
 *
 * *
 *
 * <p><strong>Note:</strong> gRPC services are inherently registered as Singletons. Ensure your
 * service implementations are thread-safe and do not hold request-scoped state in instance
 * variables.
 *
 * <h3>Logging</h3>
 *
 * <p>gRPC internally uses {@code java.util.logging}. This module automatically installs the {@link
 * SLF4JBridgeHandler} to redirect all internal gRPC logs to your configured SLF4J backend.
 */
public class GrpcModule implements Extension {
  private final List<BindableService> services = new ArrayList<>();
  private final List<Class<? extends BindableService>> serviceClasses = new ArrayList<>();

  static {
    // Optionally remove existing handlers attached to the j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    // Install the SLF4J bridge
    SLF4JBridgeHandler.install();
  }

  /**
   * Creates a new gRPC module with pre-instantiated service objects. * @param services One or more
   * fully instantiated gRPC services.
   */
  public GrpcModule(BindableService... services) {
    this.services.addAll(Arrays.asList(services));
  }

  /**
   * Creates a new gRPC module with service classes to be provisioned via Dependency Injection.
   * * @param serviceClasses One or more gRPC service classes to be resolved from the Jooby
   * registry.
   */
  @SafeVarargs
  public GrpcModule(Class<? extends BindableService>... serviceClasses) {
    bind(serviceClasses);
  }

  /**
   * Registers additional gRPC service classes to be provisioned via Dependency Injection. * @param
   * serviceClasses One or more gRPC service classes to be resolved from the Jooby registry.
   *
   * @return This module for chaining.
   */
  @SafeVarargs
  public final GrpcModule bind(Class<? extends BindableService>... serviceClasses) {
    this.serviceClasses.addAll(List.of(serviceClasses));
    return this;
  }

  /**
   * Installs the gRPC extension into the Jooby application. *
   *
   * <p>This method sets up the {@link GrpcProcessor} SPI, registers native fallback routes, and
   * defers DI resolution and the starting of the embedded in-process gRPC server to the {@code
   * onStarting} lifecycle hook. * @param app The target Jooby application.
   *
   * @throws Exception If an error occurs during installation.
   */
  @Override
  public void install(@NonNull Jooby app) throws Exception {
    var serverName = app.getName();
    var builder = InProcessServerBuilder.forName(serverName);
    final Map<String, MethodDescriptor<?, ?>> registry = new HashMap<>();

    // 1. Register user-provided services
    for (var service : services) {
      bindService(app, builder, registry, service);
    }

    var services = app.getServices();
    var processor = new DefaultGrpcProcessor(registry);
    services.put(GrpcProcessor.class, processor);

    // Lazy init service from DI.
    app.onStarting(
        () -> {
          for (Class<? extends BindableService> serviceClass : serviceClasses) {
            var service = app.require(serviceClass);
            bindService(app, builder, registry, service);
          }
          var grpcServer = builder.build().start();

          // KEEP .directExecutor() here!
          // This ensures that when the background gRPC worker finishes, it instantly pushes
          // the response back to Undertow/Netty without wasting time on another thread hop.
          var channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
          processor.setChannel(channel);

          app.onStop(channel::shutdownNow);
          app.onStop(grpcServer::shutdownNow);
        });
  }

  /**
   * Internal helper to register a service with the gRPC builder, extract its method descriptors,
   * and map a fail-fast route in the Jooby router.
   *
   * @param app The target Jooby application.
   * @param server The in-process server builder.
   * @param registry The method descriptor registry.
   * @param service The provisioned gRPC service to bind.
   */
  private static void bindService(
      Jooby app,
      InProcessServerBuilder server,
      Map<String, MethodDescriptor<?, ?>> registry,
      BindableService service) {
    server.addService(service);
    for (var method : service.bindService().getMethods()) {
      var descriptor = method.getMethodDescriptor();
      String methodFullName = descriptor.getFullMethodName();
      registry.put(methodFullName, descriptor);
      String routePath = "/" + methodFullName;

      // Map a fallback route. If a request hits this, it means the native SPI interceptor
      // failed to upgrade the request, typically due to a missing HTTP/2 configuration.
      app.post(
          routePath,
          ctx -> {
            throw new IllegalStateException(
                "gRPC request reached the standard HTTP router for: "
                    + routePath
                    + ". "
                    + "This means the native gRPC server interceptor was bypassed. "
                    + "Ensure you are running with HTTP/2 enabled, "
                    + "and that the GrpcProcessor SPI is correctly loaded.");
          });
    }
  }
}
