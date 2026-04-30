/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.jsonrpc.JsonRpcErrorCode;
import io.jooby.jsonrpc.JsonRpcException;
import io.jooby.jsonrpc.JsonRpcRequest;
import io.jooby.jsonrpc.JsonRpcResponse;
import io.jooby.jsonrpc.JsonRpcService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked"})
class JsonRpcExecutorTest {

  @Mock private Context ctx;
  @Mock private Router router;
  @Mock private JsonRpcRequest request;
  @Mock private Logger defaultLogger;
  @Mock private Logger serviceLogger;
  @Mock private JsonRpcService service;

  private Map<String, JsonRpcService> services;
  private Map<Class<?>, Logger> loggers;
  private Map<Class<?>, JsonRpcErrorCode> customMappings;

  @BeforeEach
  void setUp() throws Exception {
    services = new HashMap<>();
    loggers = new HashMap<>();
    customMappings = new HashMap<>();

    loggers.put(JsonRpcService.class, defaultLogger);

    lenient().when(ctx.getRouter()).thenReturn(router);
    lenient().when(ctx.require(any(Reified.class))).thenReturn(customMappings);
  }

  @Test
  void shouldReturnParseErrorWhenParseErrorIsPassedInConstructor() throws Exception {
    Exception parseError = new RuntimeException("Syntax error");
    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, parseError);

    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);

    assertTrue(responseOpt.isPresent());
    JsonRpcResponse response = responseOpt.get();
    assertEquals(JsonRpcErrorCode.PARSE_ERROR.getCode(), response.getError().getCode());
  }

  @Test
  void shouldReturnInvalidRequestWhenRequestIsInvalid() throws Exception {
    when(request.isValid()).thenReturn(false);
    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);

    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);

    assertTrue(responseOpt.isPresent());
    JsonRpcResponse response = responseOpt.get();
    assertEquals(JsonRpcErrorCode.INVALID_REQUEST.getCode(), response.getError().getCode());
  }

  @Test
  void shouldReturnEmptyForValidNotificationOnUnknownMethod() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("unknown.method");
    when(request.getId()).thenReturn(null);

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);

    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);

    assertFalse(responseOpt.isPresent()); // Notifications for unknown methods just drop silently
  }

  @Test
  void shouldReturnMethodNotFoundForValidMethodCallOnUnknownMethod() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("unknown.method");
    when(request.getId()).thenReturn(1);

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);

    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);

    assertTrue(responseOpt.isPresent());
    JsonRpcResponse response = responseOpt.get();
    assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND.getCode(), response.getError().getCode());
  }

  @Test
  void shouldReturnEmptyForValidNotificationOnKnownMethod() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("known.method");
    when(request.getId()).thenReturn(null);
    services.put("known.method", service);
    loggers.put(service.getClass(), serviceLogger);

    when(service.execute(ctx, request)).thenReturn("resultData");

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);

    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);

    assertFalse(responseOpt.isPresent()); // Success notifications return empty
    verify(service).execute(ctx, request);
  }

  @Test
  void shouldReturnSuccessResponseForValidMethodCallOnKnownMethod() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("known.method");
    when(request.getId()).thenReturn("req-1");
    services.put("known.method", service);
    loggers.put(service.getClass(), serviceLogger);

    when(service.execute(ctx, request)).thenReturn("resultData");

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);

    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);

    assertTrue(responseOpt.isPresent());
    JsonRpcResponse response = responseOpt.get();
    assertEquals("req-1", response.getId());
    assertEquals("resultData", response.getResult());
  }

  // --- Exception Handling, Fallbacks & Logging Branches ---

  @Test
  void exceptionShouldFallbackToRouterStatusAndLogAsError() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("fail.method");
    when(request.getId()).thenReturn(2);
    services.put("fail.method", service);
    loggers.put(service.getClass(), serviceLogger);

    RuntimeException ex = new RuntimeException("Generic crash");
    when(service.execute(ctx, request)).thenThrow(ex);

    // Fallback to router logic
    when(router.errorCode(ex)).thenReturn(StatusCode.SERVER_ERROR);

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);
    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);

    assertTrue(responseOpt.isPresent());
    assertEquals(JsonRpcErrorCode.INTERNAL_ERROR.getCode(), responseOpt.get().getError().getCode());

    // Verifies INTERNAL_ERROR triggers log.error(...)
    verify(serviceLogger)
        .error(
            anyString(),
            eq("server"),
            eq(-32603),
            eq("Internal error"),
            eq("fail.method"),
            eq(2),
            eq(ex));
  }

  @Test
  void exceptionShouldUseCustomMappingAndLogAsWarn() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("mapped.method");
    when(request.getId()).thenReturn(3);
    services.put("mapped.method", service);
    loggers.put(service.getClass(), serviceLogger);

    IllegalArgumentException ex = new IllegalArgumentException("Bad input");
    when(service.execute(ctx, request)).thenThrow(ex);

    // Add custom mapping
    customMappings.put(IllegalArgumentException.class, JsonRpcErrorCode.INVALID_PARAMS);

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);
    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);

    assertTrue(responseOpt.isPresent());
    assertEquals(JsonRpcErrorCode.INVALID_PARAMS.getCode(), responseOpt.get().getError().getCode());

    // Verifies default category with non-JsonRpcException triggers log.warn(...) with cause
    verify(serviceLogger)
        .warn(
            anyString(),
            eq("client"),
            eq(-32602),
            eq("Invalid params"),
            eq("mapped.method"),
            eq(3),
            eq(ex));
  }

  @Test
  void jsonRpcExceptionShouldExtractDirectlyAndLogAsWarnWithoutCause() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("direct.method");
    when(request.getId()).thenReturn(4);
    services.put("direct.method", service);
    loggers.put(service.getClass(), serviceLogger);

    JsonRpcException ex = new JsonRpcException(JsonRpcErrorCode.CONFLICT, "State conflict");
    when(service.execute(ctx, request)).thenThrow(ex);

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);
    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);

    assertTrue(responseOpt.isPresent());
    assertEquals(JsonRpcErrorCode.CONFLICT.getCode(), responseOpt.get().getError().getCode());

    // Verifies default category with JsonRpcException triggers log.warn(...) WITHOUT cause
    verify(serviceLogger)
        .warn(anyString(), eq("client"), eq(-32009), eq("Conflict"), eq("direct.method"), eq(4));
  }

  @Test
  void authErrorsShouldLogAtDebugLevel() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("auth.method");
    when(request.getId()).thenReturn(5);
    services.put("auth.method", service);
    loggers.put(service.getClass(), serviceLogger);

    JsonRpcException ex = new JsonRpcException(JsonRpcErrorCode.UNAUTHORIZED, "Not logged in");
    when(service.execute(ctx, request)).thenThrow(ex);

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);
    executor.proceed(ctx, request);

    // Verifies UNAUTHORIZED triggers log.debug(...)
    verify(serviceLogger)
        .debug(
            anyString(),
            eq("client"),
            eq(-32001),
            eq("Unauthorized"),
            eq("auth.method"),
            eq(5),
            eq(ex));
  }

  @Test
  void fatalExceptionShouldBubbleUpDirectly() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("fatal.method");
    services.put("fatal.method", service);

    // FIX: Add the logger mapping for the service
    loggers.put(service.getClass(), serviceLogger);

    // OutOfMemoryError is a VirtualMachineError -> Fatal
    OutOfMemoryError fatalEx = new OutOfMemoryError("Heap space");
    when(service.execute(ctx, request)).thenThrow(fatalEx);

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);

    assertThrows(OutOfMemoryError.class, () -> executor.proceed(ctx, request));
  }

  @Test
  void nestedFatalExceptionShouldBubbleUpDirectly() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("fatal.cause.method");
    services.put("fatal.cause.method", service);

    // FIX: Add the logger mapping for the service
    loggers.put(service.getClass(), serviceLogger);

    OutOfMemoryError fatalEx = new OutOfMemoryError("Heap space");
    RuntimeException wrapperEx = new RuntimeException("Wrapped", fatalEx);
    when(service.execute(ctx, request)).thenThrow(wrapperEx);

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);

    // SneakyThrows.propagate(ex.getCause()) will throw the inner OutOfMemoryError
    assertThrows(OutOfMemoryError.class, () -> executor.proceed(ctx, request));
  }

  @Test
  void notificationExceptionShouldReturnEmptyUnlessParseOrInvalidRequest() throws Exception {
    when(request.isValid()).thenReturn(true);
    when(request.getMethod()).thenReturn("notify.fail");
    when(request.getId()).thenReturn(null); // Notification
    services.put("notify.fail", service);

    // FIX: Add the logger mapping for the service so it doesn't return null
    loggers.put(service.getClass(), serviceLogger);

    when(service.execute(ctx, request))
        .thenThrow(new JsonRpcException(JsonRpcErrorCode.INTERNAL_ERROR, "Hidden error"));

    JsonRpcExecutor executor = new JsonRpcExecutor(services, loggers, null);

    // Returns empty because the client doesn't care about notification errors
    Optional<JsonRpcResponse> responseOpt = executor.proceed(ctx, request);
    assertFalse(responseOpt.isPresent());
  }
}
