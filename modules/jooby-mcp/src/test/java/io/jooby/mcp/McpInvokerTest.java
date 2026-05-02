/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.SneakyThrows;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

@ExtendWith(MockitoExtension.class)
class McpInvokerTest {

  @Mock McpSyncServerExchange exchange;
  @Mock McpTransportContext transportContext;
  @Mock McpChain finalChain;

  private final String opId = "test/op";
  private final String cls = "TestClass";
  private final String mthd = "testMethod";

  // Invoker that proceeds normally
  private final McpInvoker passThroughInvoker =
      new McpInvoker() {
        @Override
        public <R> R invoke(
            @Nullable McpSyncServerExchange ex,
            McpTransportContext tc,
            McpOperation op,
            McpChain next)
            throws Exception {
          return next.proceed(ex, tc, op);
        }
      };

  // Invoker that throws an exception to test SneakyThrows catch blocks
  private final McpInvoker throwingInvoker =
      new McpInvoker() {
        @Override
        public <R> R invoke(
            @Nullable McpSyncServerExchange ex,
            McpTransportContext tc,
            McpOperation op,
            McpChain next)
            throws Exception {
          throw new IOException("Simulated Invoker Error");
        }
      };

  @BeforeEach
  void setup() {
    // Some stateful handlers call exchange.transportContext()
    org.mockito.Mockito.lenient().when(exchange.transportContext()).thenReturn(transportContext);
  }

  // --- TOOL HANDLERS ---

  @Test
  void testAsToolHandler_Success() {
    var req = mock(McpSchema.CallToolRequest.class);
    var expectedResult = mock(McpSchema.CallToolResult.class);

    SneakyThrows.Function3<
            McpSyncServerExchange, McpTransportContext, McpOperation, McpSchema.CallToolResult>
        fn =
            (ex, tc, op) -> {
              assertEquals(exchange, ex);
              assertEquals(transportContext, tc);
              assertNotNull(op);
              return expectedResult;
            };

    var handler = passThroughInvoker.asToolHandler(opId, cls, mthd, fn);
    var actualResult = handler.apply(exchange, req);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void testAsToolHandler_Exception() {
    var req = mock(McpSchema.CallToolRequest.class);
    var handler = throwingInvoker.asToolHandler(opId, cls, mthd, (ex, tc, op) -> null);

    assertThrows(Exception.class, () -> handler.apply(exchange, req));
  }

  @Test
  void testAsStatelessToolHandler_Success() {
    var req = mock(McpSchema.CallToolRequest.class);
    var expectedResult = mock(McpSchema.CallToolResult.class);

    SneakyThrows.Function3<
            McpSyncServerExchange, McpTransportContext, McpOperation, McpSchema.CallToolResult>
        fn =
            (ex, tc, op) -> {
              assertNull(ex); // Stateless must be null
              assertEquals(transportContext, tc);
              assertNotNull(op);
              return expectedResult;
            };

    var handler = passThroughInvoker.asStatelessToolHandler(opId, cls, mthd, fn);
    var actualResult = handler.apply(transportContext, req);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void testAsStatelessToolHandler_Exception() {
    var req = mock(McpSchema.CallToolRequest.class);
    var handler = throwingInvoker.asStatelessToolHandler(opId, cls, mthd, (ex, tc, op) -> null);

    assertThrows(Exception.class, () -> handler.apply(transportContext, req));
  }

  // --- PROMPT HANDLERS ---

  @Test
  void testAsPromptHandler_Success() {
    var req = mock(McpSchema.GetPromptRequest.class);
    var expectedResult = mock(McpSchema.GetPromptResult.class);

    SneakyThrows.Function3<
            McpSyncServerExchange, McpTransportContext, McpOperation, McpSchema.GetPromptResult>
        fn =
            (ex, tc, op) -> {
              assertEquals(exchange, ex);
              assertEquals(transportContext, tc);
              assertNotNull(op);
              return expectedResult;
            };

    var handler = passThroughInvoker.asPromptHandler(opId, cls, mthd, fn);
    var actualResult = handler.apply(exchange, req);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void testAsPromptHandler_Exception() {
    var req = mock(McpSchema.GetPromptRequest.class);
    var handler = throwingInvoker.asPromptHandler(opId, cls, mthd, (ex, tc, op) -> null);

    assertThrows(Exception.class, () -> handler.apply(exchange, req));
  }

  @Test
  void testAsStatelessPromptHandler_Success() {
    var req = mock(McpSchema.GetPromptRequest.class);
    var expectedResult = mock(McpSchema.GetPromptResult.class);

    SneakyThrows.Function3<
            McpSyncServerExchange, McpTransportContext, McpOperation, McpSchema.GetPromptResult>
        fn =
            (ex, tc, op) -> {
              assertNull(ex); // Stateless must be null
              assertEquals(transportContext, tc);
              assertNotNull(op);
              return expectedResult;
            };

    var handler = passThroughInvoker.asStatelessPromptHandler(opId, cls, mthd, fn);
    var actualResult = handler.apply(transportContext, req);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void testAsStatelessPromptHandler_Exception() {
    var req = mock(McpSchema.GetPromptRequest.class);
    var handler = throwingInvoker.asStatelessPromptHandler(opId, cls, mthd, (ex, tc, op) -> null);

    assertThrows(Exception.class, () -> handler.apply(transportContext, req));
  }

  // --- RESOURCE HANDLERS ---

  @Test
  void testAsResourceHandler_Success() {
    var req = mock(McpSchema.ReadResourceRequest.class);
    var expectedResult = mock(McpSchema.ReadResourceResult.class);

    SneakyThrows.Function3<
            McpSyncServerExchange, McpTransportContext, McpOperation, McpSchema.ReadResourceResult>
        fn =
            (ex, tc, op) -> {
              assertEquals(exchange, ex);
              assertEquals(transportContext, tc);
              assertNotNull(op);
              return expectedResult;
            };

    var handler = passThroughInvoker.asResourceHandler(opId, cls, mthd, fn);
    var actualResult = handler.apply(exchange, req);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void testAsResourceHandler_Exception() {
    var req = mock(McpSchema.ReadResourceRequest.class);
    var handler = throwingInvoker.asResourceHandler(opId, cls, mthd, (ex, tc, op) -> null);

    assertThrows(Exception.class, () -> handler.apply(exchange, req));
  }

  @Test
  void testAsStatelessResourceHandler_Success() {
    var req = mock(McpSchema.ReadResourceRequest.class);
    var expectedResult = mock(McpSchema.ReadResourceResult.class);

    SneakyThrows.Function3<
            McpSyncServerExchange, McpTransportContext, McpOperation, McpSchema.ReadResourceResult>
        fn =
            (ex, tc, op) -> {
              assertNull(ex); // Stateless must be null
              assertEquals(transportContext, tc);
              assertNotNull(op);
              return expectedResult;
            };

    var handler = passThroughInvoker.asStatelessResourceHandler(opId, cls, mthd, fn);
    var actualResult = handler.apply(transportContext, req);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void testAsStatelessResourceHandler_Exception() {
    var req = mock(McpSchema.ReadResourceRequest.class);
    var handler = throwingInvoker.asStatelessResourceHandler(opId, cls, mthd, (ex, tc, op) -> null);

    assertThrows(Exception.class, () -> handler.apply(transportContext, req));
  }

  // --- COMPLETION HANDLERS ---

  @Test
  void testAsCompletionHandler_Success() {
    var req = mock(McpSchema.CompleteRequest.class);
    var expectedResult = mock(McpSchema.CompleteResult.class);

    SneakyThrows.Function3<
            McpSyncServerExchange, McpTransportContext, McpOperation, McpSchema.CompleteResult>
        fn =
            (ex, tc, op) -> {
              assertEquals(exchange, ex);
              assertEquals(transportContext, tc);
              assertNotNull(op);
              return expectedResult;
            };

    var handler = passThroughInvoker.asCompletionHandler(opId, cls, mthd, fn);
    var actualResult = handler.apply(exchange, req);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void testAsCompletionHandler_Exception() {
    var req = mock(McpSchema.CompleteRequest.class);
    var handler = throwingInvoker.asCompletionHandler(opId, cls, mthd, (ex, tc, op) -> null);

    assertThrows(Exception.class, () -> handler.apply(exchange, req));
  }

  @Test
  void testAsStatelessCompletionHandler_Success() {
    var req = mock(McpSchema.CompleteRequest.class);
    var expectedResult = mock(McpSchema.CompleteResult.class);

    SneakyThrows.Function3<
            McpSyncServerExchange, McpTransportContext, McpOperation, McpSchema.CompleteResult>
        fn =
            (ex, tc, op) -> {
              assertNull(ex); // Stateless must be null
              assertEquals(transportContext, tc);
              assertNotNull(op);
              return expectedResult;
            };

    var handler = passThroughInvoker.asStatelessCompletionHandler(opId, cls, mthd, fn);
    var actualResult = handler.apply(transportContext, req);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void testAsStatelessCompletionHandler_Exception() {
    var req = mock(McpSchema.CompleteRequest.class);
    var handler =
        throwingInvoker.asStatelessCompletionHandler(opId, cls, mthd, (ex, tc, op) -> null);

    assertThrows(Exception.class, () -> handler.apply(transportContext, req));
  }

  // --- CHAINING (THEN) ---

  @Test
  void testThen_ChainsCorrectly() throws Exception {
    McpInvoker firstInvoker = mock(McpInvoker.class);
    McpInvoker secondInvoker = mock(McpInvoker.class);
    McpOperation operation = mock(McpOperation.class);

    when(finalChain.proceed(exchange, transportContext, operation)).thenReturn("Done");

    // The first invoker executes its body, then calls chain.proceed()
    when(firstInvoker.invoke(any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              McpChain nextChain = inv.getArgument(3);
              return nextChain.proceed(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2));
            });

    // The second invoker executes its body, then calls chain.proceed()
    when(secondInvoker.invoke(any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              McpChain nextChain = inv.getArgument(3);
              return nextChain.proceed(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2));
            });

    // We start with the passThroughInvoker, chain it to the first mock, then to the second mock.
    McpInvoker chained = passThroughInvoker.then(firstInvoker).then(secondInvoker);

    Object result = chained.invoke(exchange, transportContext, operation, finalChain);

    assertEquals("Done", result);
    verify(firstInvoker)
        .invoke(eq(exchange), eq(transportContext), eq(operation), any(McpChain.class));
    verify(secondInvoker)
        .invoke(eq(exchange), eq(transportContext), eq(operation), any(McpChain.class));
    verify(finalChain).proceed(exchange, transportContext, operation);
  }

  @Test
  void testThen_NullNextInvoker() {
    assertThrows(NullPointerException.class, () -> passThroughInvoker.then(null));
  }
}
