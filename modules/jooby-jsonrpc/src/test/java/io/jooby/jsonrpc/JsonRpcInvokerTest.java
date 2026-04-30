/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.jooby.Context;

class JsonRpcInvokerTest {

  @Test
  void shouldRequireNonNullNextInvoker() {
    JsonRpcInvoker root = (ctx, request, next) -> Optional.empty();

    NullPointerException thrown = assertThrows(NullPointerException.class, () -> root.then(null));

    assertEquals("next invoker is required", thrown.getMessage());
  }

  @Test
  void shouldComposeInvokersInCorrectOrder() {
    List<String> executionOrder = new ArrayList<>();

    // Invoker A wraps the entire process
    JsonRpcInvoker invokerA =
        (ctx, request, next) -> {
          executionOrder.add("A-pre");
          Optional<JsonRpcResponse> response = next.proceed(ctx, request);
          executionOrder.add("A-post");
          return response;
        };

    // Invoker B is inside A
    JsonRpcInvoker invokerB =
        (ctx, request, next) -> {
          executionOrder.add("B-pre");
          Optional<JsonRpcResponse> response = next.proceed(ctx, request);
          executionOrder.add("B-post");
          return response;
        };

    // The final chain represents the actual RPC method call at the end of the pipeline
    JsonRpcChain finalChain =
        (c, r) -> {
          executionOrder.add("TargetMethod");
          return Optional.of(mock(JsonRpcResponse.class));
        };

    // Compose A -> B
    JsonRpcInvoker composed = invokerA.then(invokerB);

    // Execute the composed pipeline
    Context mockCtx = mock(Context.class);
    JsonRpcRequest mockReq = mock(JsonRpcRequest.class);

    Optional<JsonRpcResponse> result = composed.invoke(mockCtx, mockReq, finalChain);

    // Verify a response successfully bubbled up
    assertTrue(result.isPresent());

    // Verify the "Russian Doll" execution order:
    // A starts -> B starts -> Target executes -> B finishes -> A finishes
    List<String> expectedOrder = List.of("A-pre", "B-pre", "TargetMethod", "B-post", "A-post");

    assertEquals(expectedOrder, executionOrder);
  }
}
