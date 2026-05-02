/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.StatusCode;

@ExtendWith(MockitoExtension.class)
class TrpcErrorHandlerTest {

  @Mock Context ctx;

  private TrpcErrorHandler errorHandler;

  @BeforeEach
  void setup() {
    errorHandler = new TrpcErrorHandler();
  }

  @Test
  void testApply_IgnoresNonTrpcPaths() {
    when(ctx.getRequestPath()).thenReturn("/api/movies");
    Throwable cause = new RuntimeException("Generic Error");

    errorHandler.apply(ctx, cause, StatusCode.SERVER_ERROR);

    // Verifies the handler aborted immediately and did not manipulate the response
    verify(ctx, never()).setResponseCode(any());
    verify(ctx, never()).render(any());
  }

  @Test
  void testApply_WithExistingTrpcException() {
    when(ctx.getRequestPath()).thenReturn("/trpc/movies.getById");
    when(ctx.setResponseCode(StatusCode.UNAUTHORIZED)).thenReturn(ctx);

    // The exception is already a TrpcException, so no map lookups should occur
    TrpcException cause = new TrpcException("movies.getById", TrpcErrorCode.UNAUTHORIZED);

    errorHandler.apply(ctx, cause, StatusCode.SERVER_ERROR);

    verify(ctx).setResponseCode(StatusCode.UNAUTHORIZED);
    verify(ctx).render(cause.toMap());
  }

  @Test
  void testApply_WithCustomMapping_ExactMatch() {
    when(ctx.getRequestPath()).thenReturn("/trpc/movies.create");
    when(ctx.setResponseCode(StatusCode.PRECONDITION_FAILED)).thenReturn(ctx);

    IllegalArgumentException cause = new IllegalArgumentException("Invalid rating");

    // Provide a custom exception mapping that catches IllegalArgumentException
    Map customMappings = new LinkedHashMap<>();
    customMappings.put(IllegalArgumentException.class, TrpcErrorCode.PRECONDITION_FAILED);
    Reified<Map<Object, Object>> reified = Reified.map(Class.class, TrpcErrorCode.class);
    when(ctx.require(reified)).thenReturn(customMappings);

    errorHandler.apply(ctx, cause, StatusCode.SERVER_ERROR);

    verify(ctx).setResponseCode(StatusCode.PRECONDITION_FAILED);

    // Capture the rendered map to verify the custom mapping successfully transformed the envelope
    ArgumentCaptor<Object> renderCaptor = ArgumentCaptor.forClass(Object.class);
    verify(ctx).render(renderCaptor.capture());

    Map<String, Object> responseMap = (Map<String, Object>) renderCaptor.getValue();
    Map<String, Object> errorNode = (Map<String, Object>) responseMap.get("error");
    Map<String, Object> dataNode = (Map<String, Object>) errorNode.get("data");

    assertEquals("PRECONDITION_FAILED", dataNode.get("code"));
    assertEquals("movies.create", dataNode.get("path"));
  }

  @Test
  void testApply_WithCustomMapping_NoMatch_FallsBackToDefaultStatusCode() {
    when(ctx.getRequestPath()).thenReturn("/trpc/movies.delete");
    when(ctx.setResponseCode(StatusCode.NOT_FOUND))
        .thenReturn(ctx); // TrpcErrorCode.of(NOT_FOUND) maps to NOT_FOUND

    NullPointerException cause = new NullPointerException("Missing ID");

    // Provide a mapping, but for a DIFFERENT exception type
    Map customMappings = new LinkedHashMap<>();
    customMappings.put(IllegalArgumentException.class, TrpcErrorCode.PRECONDITION_FAILED);
    Reified<Map<Object, Object>> reified = Reified.map(Class.class, TrpcErrorCode.class);
    when(ctx.require(reified)).thenReturn(customMappings);

    errorHandler.apply(ctx, cause, StatusCode.NOT_FOUND);

    verify(ctx).setResponseCode(StatusCode.NOT_FOUND);

    ArgumentCaptor<Object> renderCaptor = ArgumentCaptor.forClass(Object.class);
    verify(ctx).render(renderCaptor.capture());

    Map<String, Object> responseMap = (Map<String, Object>) renderCaptor.getValue();
    Map<String, Object> errorNode = (Map<String, Object>) responseMap.get("error");
    Map<String, Object> dataNode = (Map<String, Object>) errorNode.get("data");

    // Verifies it ignored the mismatching custom mapping and used the provided Jooby StatusCode
    assertEquals("NOT_FOUND", dataNode.get("code"));
  }

  @Test
  void testApply_WithEmptyCustomMapping_FallsBackToDefaultStatusCode() {
    when(ctx.getRequestPath()).thenReturn("/trpc/users.update");
    when(ctx.setResponseCode(StatusCode.BAD_REQUEST)).thenReturn(ctx);

    Exception cause = new Exception("General failure");

    // Provide a completely empty mapping to hit the for-loop bypass
    Reified<Map<Object, Object>> reified = Reified.map(Class.class, TrpcErrorCode.class);
    when(ctx.require(reified)).thenReturn(Collections.emptyMap());

    errorHandler.apply(ctx, cause, StatusCode.BAD_REQUEST);

    verify(ctx).setResponseCode(StatusCode.BAD_REQUEST);
    verify(ctx).render(anyMap());
  }
}
