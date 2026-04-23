/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.jsonrpc.*;

/**
 * The internal execution engine and "final invoker" for JSON-RPC requests.
 *
 * <p>This class acts as the terminal end of the {@link JsonRpcChain}. It is responsible for the
 * final stages of the JSON-RPC lifecycle:
 *
 * <ul>
 *   <li>Validating the parsed request envelope.
 *   <li>Routing the request to the appropriate {@link JsonRpcService}.
 *   <li>Executing the target method.
 *   <li>Acting as the ultimate safety net by catching all exceptions and translating them into
 *       compliant {@link JsonRpcResponse} objects.
 * </ul>
 */
public class JsonRpcExecutor implements JsonRpcChain {
  private final Map<String, JsonRpcService> services;
  private final Map<Class<?>, Logger> loggers;
  private final JsonRpcExceptionTranslator exceptionTranslator;
  private final Exception parseError;

  /**
   * Constructs a new executor for a single JSON-RPC request.
   *
   * @param loggers A map of loggers keyed by service class.
   * @param services A map of registered JSON-RPC services keyed by method name.
   * @param exceptionTranslator The translator used to map standard Throwables to JSON-RPC error
   *     codes.
   * @param parseError Any exception that occurred during the initial JSON parsing phase.
   */
  public JsonRpcExecutor(
      Map<Class<?>, Logger> loggers,
      Map<String, JsonRpcService> services,
      JsonRpcExceptionTranslator exceptionTranslator,
      Exception parseError) {
    this.services = services;
    this.loggers = loggers;
    this.exceptionTranslator = exceptionTranslator;
    this.parseError = parseError;
  }

  /**
   * Executes the JSON-RPC request and returns an optional response.
   *
   * <p>This method adheres strictly to the JSON-RPC 2.0 specification regarding error handling and
   * response generation. It will return {@link Optional#empty()} for Notifications, unless a
   * fundamental Parse Error or Invalid Request error occurs, which always require a response.
   *
   * @param ctx The current HTTP context passed down the chain.
   * @param request The incoming JSON-RPC request passed down the chain.
   * @return An Optional containing the JSON-RPC response, or empty if the request was a valid
   *     Notification.
   */
  @Override
  public @NonNull Optional<JsonRpcResponse> proceed(
      @NonNull Context ctx, @NonNull JsonRpcRequest request) {
    var log = loggers.get(JsonRpcService.class);
    try {
      if (parseError != null) {
        throw new JsonRpcException(JsonRpcErrorCode.PARSE_ERROR, parseError);
      }
      if (!request.isValid()) {
        throw new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, "Invalid JSON-RPC request");
      }
      var fullMethod = request.getMethod();
      var targetService = services.get(fullMethod);
      if (targetService != null) {
        log = loggers.get(targetService.getClass());
        var result = targetService.execute(ctx, request);
        return request.getId() != null
            ? Optional.of(JsonRpcResponse.success(request.getId(), result))
            : Optional.empty();
      }
      if (request.getId() == null) {
        return Optional.empty();
      }
      throw new JsonRpcException(
          JsonRpcErrorCode.METHOD_NOT_FOUND, "Method not found: " + fullMethod);
    } catch (Throwable cause) {
      return toRpcResponse(log, request, cause);
    }
  }

  private Optional<JsonRpcResponse> toRpcResponse(
      Logger log, JsonRpcRequest request, Throwable ex) {
    var code = exceptionTranslator.toErrorCode(ex);
    log(log, request, code, ex);

    if (SneakyThrows.isFatal(ex)) {
      throw SneakyThrows.propagate(ex);
    } else if (ex.getCause() != null && SneakyThrows.isFatal(ex.getCause())) {
      throw SneakyThrows.propagate(ex.getCause());
    }

    if (request.getId() != null) {
      return Optional.of(JsonRpcResponse.error(request.getId(), code, ex));
    } else if (code == JsonRpcErrorCode.PARSE_ERROR || code == JsonRpcErrorCode.INVALID_REQUEST) {
      // must return a valid response even if the request is invalid
      return Optional.of(JsonRpcResponse.error(null, code, ex));
    }
    return Optional.empty();
  }

  /**
   * Logs JSON-RPC errors adaptively based on the error code.
   *
   * <p>Internal server errors are logged as standard errors. Authorization and routing errors are
   * logged at debug level to prevent log flooding. Other application errors are logged as warnings.
   *
   * @param log The logger instance to use.
   * @param request The request that triggered the error.
   * @param code The error code.
   * @param cause The underlying exception.
   */
  private void log(Logger log, JsonRpcRequest request, JsonRpcErrorCode code, Throwable cause) {
    var type = code == JsonRpcErrorCode.INTERNAL_ERROR ? "server" : "client";
    var message = "JSON-RPC {} error [{} {}] on method '{}' (id: {})";
    switch (code) {
      case INTERNAL_ERROR ->
          log.error(
              message,
              type,
              code.getCode(),
              code.getMessage(),
              request.getMethod(),
              request.getId(),
              cause);
      case UNAUTHORIZED, FORBIDDEN, NOT_FOUND_ERROR ->
          log.debug(
              message,
              type,
              code.getCode(),
              code.getMessage(),
              request.getMethod(),
              request.getId(),
              cause);
      default -> {
        if (cause instanceof JsonRpcException) {
          log.warn(
              message,
              type,
              code.getCode(),
              code.getMessage(),
              request.getMethod(),
              request.getId());
        } else {
          log.warn(
              message,
              type,
              code.getCode(),
              code.getMessage(),
              request.getMethod(),
              request.getId(),
              cause);
        }
      }
    }
  }
}
