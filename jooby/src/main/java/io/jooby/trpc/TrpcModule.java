/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

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
 * install(new JacksonModule());
 *
 * // 2. Install the tRPC extension
 * install(new TrpcModule());
 *
 * // 3. Register your @Trpc annotated controllers
 * mvc(new MovieService_());
 * }
 * }</pre>
 */
public class TrpcModule implements Extension {

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
    var services = app.getServices();

    // Ensure a JSON module has provided the necessary parser
    services.require(TrpcParser.class);

    // Initialize the custom exception mapping registry
    services.mapOf(Class.class, TrpcErrorCode.class);

    // Register the specialized JSON-RPC error formatter
    app.error(new TrpcErrorHandler());
  }
}
