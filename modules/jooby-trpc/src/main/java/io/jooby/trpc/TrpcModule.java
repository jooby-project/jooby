/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import java.util.List;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;

/**
 * Jooby extension that enables tRPC support for the application.
 *
 * <p>This module is responsible for bootstrapping the required tRPC infrastructure, specifically
 * the specialized error handling and the parameter parsing mechanisms needed to handle tRPC network
 * payloads.
 *
 * <p><b>Prerequisites:</b>
 *
 * <p>Because tRPC relies heavily on JSON serialization, a JSON module (such as {@code
 * JacksonModule}, or {@code AvajeJsonbModule}) <b>must</b> be installed before installing this
 * module. The JSON module automatically registers the underlying {@link TrpcParser} that this
 * extension requires.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * {
 * // 1. Install a JSON engine (Prerequisite)
 * install(new Jackson3Module())
 *
 * // 2. JSON implementation of protocol
 * install(new TrpcJackson3Module());
 *
 * // 3. Install the tRPC extension
 * install(new TrpcModule(MovieServiceTrpc_()));
 * }
 * }</pre>
 */
public class TrpcModule implements Extension {

  private final String path;
  private final List<TrpcService> services;

  /**
   * Creates a new instance of {@code TrpcModule} with the specified base path and tRPC services.
   *
   * @param path The base path for all tRPC routes. This is the root URL prefix where all tRPC
   *     services will be registered. Must not be {@code null}.
   * @param service The primary tRPC service to register. Must not be {@code null}.
   * @param services Additional tRPC services to register (if any). Optional and may be omitted.
   */
  public TrpcModule(String path, TrpcService service, TrpcService... services) {
    this.path = path;
    this.services = Stream.concat(Stream.of(service), Stream.of(services)).toList();
  }

  /**
   * Constructs a new {@code TrpcModule} with the default base path: <code>trpc</code> and the
   * provided services.
   *
   * @param service The primary tRPC service to register. Must not be {@code null}.
   * @param services Additional tRPC services to register (if any). Optional and may be omitted.
   */
  public TrpcModule(TrpcService service, TrpcService... services) {
    this("/trpc", service, services);
  }

  /**
   * Installs the tRPC extension into the Jooby application.
   *
   * <p>During installation, this method performs the following setup:
   *
   * <ul>
   *   <li>Validates that a {@link TrpcParser} is available in the service registry.
   *   <li>Initializes the registry map for custom {@code Class} to {@link TrpcErrorCode} mappings,
   *       allowing developers to map domain exceptions to specific tRPC errors.
   *   <li>Registers the {@link TrpcErrorHandler} globally to intercept and correctly format
   *       exceptions thrown from {@code /trpc/*} endpoints.
   * </ul>
   *
   * @param app The current Jooby application.
   * @throws Exception If a required service (such as the {@code TrpcParser}) is missing from the
   *     registry.
   */
  @Override
  public void install(@NonNull Jooby app) throws Exception {
    var registry = app.getServices();

    // Ensure a JSON module has provided the necessary parser
    registry.require(TrpcParser.class);

    // Initialize the custom exception mapping registry
    registry.mapOf(Class.class, TrpcErrorCode.class);

    // Register the specialized JSON-RPC error formatter
    app.error(new TrpcErrorHandler());

    for (var service : services) {
      service.install(path, app);
    }
  }
}
