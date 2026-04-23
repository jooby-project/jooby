/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.util.Optional;

import io.jooby.Context;

/**
 * Represents the remaining execution pipeline for a JSON-RPC request.
 *
 * <p>This interface is a core component of the JSON-RPC middleware architecture (Chain of
 * Responsibility). When a {@link JsonRpcInvoker} executes, it is provided an instance of this
 * chain, which represents all subsequent middleware components and the final execution target.
 *
 * <p>A typical middleware implementation will:
 *
 * <ol>
 *   <li>Perform pre-processing (e.g., start a timer, evaluate security constraints).
 *   <li>Call {@link #proceed(Context, JsonRpcRequest)} to delegate execution to the next link in
 *       the chain.
 *   <li>Inspect or log the returned {@link JsonRpcResponse}.
 *   <li>Return the response back up the chain.
 * </ol>
 *
 * <p>The terminal implementation of this chain is the internal execution engine, which guarantees
 * that exceptions are caught and safely translated into standard JSON-RPC error responses.
 * Therefore, callers of {@code proceed} generally do not need to wrap the call in a try-catch block
 * for application logic.
 */
public interface JsonRpcChain {

  /**
   * Passes control to the next element in the pipeline, or executes the target JSON-RPC method if
   * this is the end of the chain.
   *
   * <p>Because the request and context are passed explicitly, a middleware component is free to
   * modify, wrap, or sanitize them before passing them down the line.
   *
   * @param ctx The current HTTP context.
   * @param request The parsed JSON-RPC request envelope.
   * @return An {@link Optional} containing the final JSON-RPC response. This will be {@link
   *     Optional#empty()} if the request was a valid Notification (which explicitly forbids a
   *     response).
   */
  Optional<JsonRpcResponse> proceed(Context ctx, JsonRpcRequest request);
}
