/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */

import org.slf4j.bridge.SLF4JBridgeHandler;

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
module io.jooby.grpc {
  exports io.jooby.grpc;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires jul.to.slf4j;
  requires io.grpc;
  requires io.grpc.inprocess;
  requires io.grpc.stub;
}
