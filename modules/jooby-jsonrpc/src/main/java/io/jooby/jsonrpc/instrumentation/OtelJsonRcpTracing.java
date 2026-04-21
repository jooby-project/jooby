/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc.instrumentation;

import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.internal.jsonrpc.JsonRpcExceptionTranslator;
import io.jooby.jsonrpc.*;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

public class OtelJsonRcpTracing implements JsonRpcInvoker {

  private final Tracer tracer;

  public OtelJsonRcpTracing(OpenTelemetry otel) {
    tracer = otel.getTracer("io.jooby.jsonrpc");
  }

  @Override
  public @NonNull Optional<JsonRpcResponse> invoke(
      @NonNull Context ctx,
      @NonNull JsonRpcRequest request,
      SneakyThrows.@NonNull Supplier<Optional<JsonRpcResponse>> action)
      throws Exception {
    var method = Optional.ofNullable(request.getMethod()).orElse(JsonRpcRequest.UNKNOWN_METHOD);
    var span =
        tracer
            .spanBuilder(request.getMethod())
            .setAttribute("rpc.system", "jsonrpc")
            .setAttribute("rpc.method", method)
            .setAttribute(
                "rpc.jsonrpc.request_id",
                Optional.ofNullable(request.getId()).map(Objects::toString).orElse(null))
            .startSpan();

    try (var scope = span.makeCurrent()) {
      var result = action.get();
      if (result.isPresent()) {
        var rsp = result.get();
        var error = rsp.getError();
        if (error == null) {
          span.setStatus(StatusCode.OK);
        } else {
          traceError(span, error.exception(), error);
        }
      }
      return result;
    } catch (JsonRpcException e) {
      traceError(span, e, e.toErrorDetail());
      throw e;
    } catch (Throwable e) {
      traceError(span, e, ctx.require(JsonRpcExceptionTranslator.class).toErrorDetail(e));
      throw e;
    } finally {
      span.end();
    }
  }

  private static void traceError(Span span, Throwable cause, JsonRpcResponse.ErrorDetail error) {
    span.setStatus(StatusCode.ERROR, error.getMessage());
    if (cause != null) {
      span.recordException(cause);
    }
  }
}
