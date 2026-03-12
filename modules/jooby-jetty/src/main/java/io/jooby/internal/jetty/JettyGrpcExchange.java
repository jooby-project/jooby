/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import io.jooby.GrpcExchange;

public class JettyGrpcExchange implements GrpcExchange {

  private final Request request;
  private final Response response;
  private final Callback jettyCallback;
  private boolean headersSent = false;

  // Create a mutable trailers object that Jetty will pull from at the end of the stream
  private final HttpFields.Mutable trailers = HttpFields.build();

  public JettyGrpcExchange(Request request, Response response, Callback jettyCallback) {
    this.request = request;
    this.response = response;
    this.jettyCallback = jettyCallback;

    response.getHeaders().put("Content-Type", "application/grpc");

    // CRITICAL FIX: Register the supplier BEFORE the response commits
    response.setTrailersSupplier(() -> trailers);
  }

  @Override
  public String getRequestPath() {
    return request.getHttpURI().getPath();
  }

  @Override
  public String getHeader(String name) {
    return request.getHeaders().get(name);
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> map = new HashMap<>();
    for (var field : request.getHeaders()) {
      map.put(field.getName(), field.getValue());
    }
    return map;
  }

  @Override
  public void send(ByteBuffer payload, Consumer<Throwable> callback) {
    headersSent = true;

    response.write(
        false,
        payload,
        new Callback() {
          @Override
          public void succeeded() {
            callback.accept(null);
          }

          @Override
          public void failed(Throwable x) {
            callback.accept(x);
          }
        });
  }

  @Override
  public void close(int statusCode, String description) {
    if (headersSent) {
      // Trailers-Appended: Data was sent, populate the mutable trailers object
      trailers.add("grpc-status", String.valueOf(statusCode));
      if (description != null) {
        trailers.add("grpc-message", description);
      }

      // Complete stream. Jetty will automatically read from the supplier we registered earlier.
      response.write(true, ByteBuffer.allocate(0), jettyCallback);
    } else {
      // Trailers-Only: No data was sent, trailers become standard HTTP headers.
      response.getHeaders().put("grpc-status", String.valueOf(statusCode));
      if (description != null) {
        response.getHeaders().put("grpc-message", description);
      }
      response.write(true, ByteBuffer.allocate(0), jettyCallback);
    }
  }
}
