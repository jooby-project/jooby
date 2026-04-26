/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Jooby;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.mcp.McpChain;
import io.jooby.mcp.McpOperation;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

public class McpExecutorTest {

  private Jooby app;
  private Router router;
  private McpExecutor executor;
  private McpTransportContext transportContext;
  private McpChain chain;
  private McpOperation operation;

  @BeforeEach
  void setUp() {
    app = mock(Jooby.class);
    router = mock(Router.class);
    when(app.getRouter()).thenReturn(router);

    executor = new McpExecutor(app);
    transportContext = mock(McpTransportContext.class);
    chain = mock(McpChain.class);
    operation = mock(McpOperation.class);

    // Setup default operation behavior
    when(operation.getClassName()).thenReturn(getClass().getName());
    when(operation.getId()).thenReturn("test-op");

    // Global router default to prevent NPE during logging
    when(router.errorCode(any())).thenReturn(StatusCode.SERVER_ERROR);
  }

  @Test
  void testInvokeSuccess() throws Throwable {
    Object result = new Object();
    when(chain.proceed(any(), eq(transportContext), eq(operation))).thenReturn(result);

    Object actual = executor.invoke(null, transportContext, operation, chain);

    assertEquals(result, actual);
  }

  @Test
  void testInvokeFatalError() throws Throwable {
    // OutOfMemoryError triggers SneakyThrows.isFatal
    OutOfMemoryError fatal = new OutOfMemoryError();
    when(chain.proceed(any(), any(), any())).thenThrow(fatal);

    // This should now propagate the fatal error after logging
    assertThrows(
        OutOfMemoryError.class, () -> executor.invoke(null, transportContext, operation, chain));

    verify(operation).exception(fatal);
  }

  @Test
  void testInvokeToolError() throws Throwable {
    Exception error = new Exception("tool-failure");
    when(chain.proceed(any(), any(), any())).thenThrow(error);
    when(operation.isTool()).thenReturn(true);

    Object result = executor.invoke(null, transportContext, operation, chain);

    assertTrue(result instanceof McpSchema.CallToolResult);
    McpSchema.CallToolResult toolResult = (McpSchema.CallToolResult) result;
    assertTrue(toolResult.isError());
    assertEquals("tool-failure", ((McpSchema.TextContent) toolResult.content().get(0)).text());
  }

  @Test
  void testInvokeMcpErrorRethrow() throws Throwable {
    McpSchema.JSONRPCResponse.JSONRPCError jsonError =
        new McpSchema.JSONRPCResponse.JSONRPCError(-32000, "mcp-error", null);
    McpError mcpError = new McpError(jsonError);

    when(chain.proceed(any(), any(), any())).thenThrow(mcpError);

    // Should rethrow the same McpError
    McpError actual =
        assertThrows(
            McpError.class, () -> executor.invoke(null, transportContext, operation, chain));

    assertEquals(jsonError, actual.getJsonRpcError());
  }

  @Test
  void testStatusCodeMapping() throws Throwable {
    checkMapping(new Exception(), StatusCode.NOT_FOUND, McpSchema.ErrorCodes.RESOURCE_NOT_FOUND);
    checkMapping(new Exception(), StatusCode.BAD_REQUEST, McpSchema.ErrorCodes.INVALID_PARAMS);
    checkMapping(new Exception(), StatusCode.CONFLICT, McpSchema.ErrorCodes.INVALID_PARAMS);
    checkMapping(new Exception(), StatusCode.FORBIDDEN, McpSchema.ErrorCodes.INTERNAL_ERROR);
  }

  @Test
  void testIsServerErrorBranching() {
    assertTrue(McpExecutor.isServerError(McpSchema.ErrorCodes.INTERNAL_ERROR));
    assertTrue(McpExecutor.isServerError(-32701));
    assertFalse(McpExecutor.isServerError(-32600));
  }

  @Test
  void testToolErrorWithNullMessage() throws Throwable {
    Exception error = new Exception((String) null);
    when(chain.proceed(any(), any(), any())).thenThrow(error);
    when(operation.isTool()).thenReturn(true);

    Object result = executor.invoke(null, transportContext, operation, chain);
    McpSchema.CallToolResult toolResult = (McpSchema.CallToolResult) result;
    assertEquals(
        "Unknown error occurred", ((McpSchema.TextContent) toolResult.content().get(0)).text());
  }

  private void checkMapping(Throwable t, StatusCode joobyCode, int expectedMcpCode)
      throws Throwable {
    reset(chain, router);
    when(chain.proceed(any(), any(), any())).thenThrow(t);
    when(router.errorCode(t)).thenReturn(joobyCode);

    McpError ex =
        assertThrows(
            McpError.class, () -> executor.invoke(null, transportContext, operation, chain));
    assertEquals(expectedMcpCode, ex.getJsonRpcError().code());
  }
}
