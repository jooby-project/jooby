/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.jsonrpc;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;

/**
 * Interface for generated JSON-RPC service glue code (*Rpc classes).
 *
 * <p>This interface allows the global {@link JsonRpcModule} to coordinate multiple JSON-RPC
 * services on a single endpoint by checking which service supports a specific method namespace.
 */
public interface JsonRpcService {

  /**
   * List all RPC method exposed by this service.
   *
   * @return All RPC method exposed by this service.
   */
  List<String> getMethods();

  /**
   * Executes the requested method using the provided context and request data.
   *
   * @param ctx The current Jooby context.
   * @param req The individual JSON-RPC request object.
   * @return The result of the method invocation.
   * @throws Exception If an error occurs during execution.
   */
  Object execute(@NonNull Context ctx, @NonNull JsonRpcRequest req) throws Exception;
}
