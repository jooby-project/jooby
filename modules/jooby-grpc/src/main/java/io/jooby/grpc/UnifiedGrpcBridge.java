/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.grpc.*;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Sender;

public class UnifiedGrpcBridge implements Route.Handler {

  // Minimal Marshaller to pass raw bytes through the bridge
  private static class RawMarshaller implements MethodDescriptor.Marshaller<byte[]> {
    @Override
    public InputStream stream(byte[] value) {
      return new ByteArrayInputStream(value);
    }

    @Override
    public byte[] parse(InputStream stream) {
      try {
        return stream.readAllBytes();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ManagedChannel channel;
  private final GrpcMethodRegistry methodRegistry;

  public UnifiedGrpcBridge(ManagedChannel channel, GrpcMethodRegistry methodRegistry) {
    this.channel = channel;
    this.methodRegistry = methodRegistry;
  }

  @Override
  public Object apply(@NonNull Context ctx) {
    // Setup gRPC response headers
    ctx.setResponseType("application/grpc");

    // Route paths: /{package.Service}/{Method}
    String path = ctx.getRequestPath();
    // Remove the leading slash to match the gRPC method registry format
    var descriptor = methodRegistry.get(path.substring(1));

    if (descriptor == null) {
      log.warn("Method not found in bridge registry: {}", path);
      ctx.setResponseTrailer("grpc-status", String.valueOf(Status.UNIMPLEMENTED.getCode().value()));
      ctx.setResponseTrailer("grpc-message", "Method not found");
      return ctx.send("");
    }

    var method =
        MethodDescriptor.<byte[], byte[]>newBuilder()
            .setType(descriptor.getType())
            .setFullMethodName(descriptor.getFullMethodName())
            .setRequestMarshaller(new RawMarshaller())
            .setResponseMarshaller(new RawMarshaller())
            .build();

    // 1. Propagate Call Options (Deadlines)
    CallOptions callOptions = extractCallOptions(ctx);

    // 2. Propagate HTTP Headers to gRPC Metadata
    Metadata metadata = extractMetadata(ctx);

    // Attach the metadata to the channel using an interceptor
    io.grpc.Channel interceptedChannel =
        io.grpc.ClientInterceptors.intercept(
            channel, io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(metadata));

    // Create the call using the intercepted channel and the configured options
    ClientCall<byte[], byte[]> call = interceptedChannel.newCall(method, callOptions);

    Sender sender = ctx.responseSender(false);
    AtomicBoolean isFinished = new AtomicBoolean(false);

    // Unified Response Observer (Handles data coming BACK from the server)
    ClientResponseObserver<byte[], byte[]> responseObserver =
        new ClientResponseObserver<>() {
          @Override
          public void beforeStart(ClientCallStreamObserver<byte[]> requestStream) {
            requestStream.disableAutoInboundFlowControl();
          }

          @Override
          public void onNext(byte[] value) {
            if (isFinished.get()) return;

            byte[] framed = addGrpcHeader(value);
            sender.write(
                framed,
                new Sender.Callback() {
                  @Override
                  public void onComplete(@NonNull Context ctx, @Nullable Throwable cause) {
                    if (cause != null) {
                      onError(cause);
                    }
                  }
                });
          }

          @Override
          public void onError(Throwable t) {
            if (isFinished.compareAndSet(false, true)) {
              log.debug("gRPC stream error", t);
              Status status = Status.fromThrowable(t);
              sender.setTrailer("grpc-status", String.valueOf(status.getCode().value()));
              if (status.getDescription() != null) {
                sender.setTrailer("grpc-message", status.getDescription());
              }
              sender.close();
            }
          }

          @Override
          public void onCompleted() {
            if (isFinished.compareAndSet(false, true)) {
              sender.setTrailer("grpc-status", "0");
              sender.close();
            }
          }
        };

    // Map gRPC Method Type to the correct ClientCalls utility
    StreamObserver<byte[]> requestObserver =
        switch (method.getType()) {
          case UNARY -> ClientCalls.asyncBidiStreamingCall(call, responseObserver);
          case BIDI_STREAMING, CLIENT_STREAMING ->
              ClientCalls.asyncBidiStreamingCall(call, responseObserver);
          case SERVER_STREAMING -> wrapServerStreaming(call, responseObserver);
          default -> null;
        };

    if (requestObserver == null) {
      ctx.setResponseTrailer("grpc-status", String.valueOf(Status.INTERNAL.getCode().value()));
      ctx.setResponseTrailer("grpc-message", "Unsupported method type");
      return ctx.send("");
    }

    // Return the reactive subscriber to let Jooby pipe the request stream into it
    return new GrpcRequestBridge(requestObserver);
  }

  /**
   * Extracts the grpc-timeout header and applies it to CallOptions. gRPC timeout format:
   * {TimeoutValue}{TimeoutUnit} (e.g., 100m = 100 milliseconds, 10S = 10 seconds).
   */
  private CallOptions extractCallOptions(Context ctx) {
    CallOptions options = CallOptions.DEFAULT;
    String timeout = ctx.header("grpc-timeout").valueOrNull();

    if (timeout == null || timeout.isEmpty()) {
      return options;
    }

    try {
      char unit = timeout.charAt(timeout.length() - 1);
      long value = Long.parseLong(timeout.substring(0, timeout.length() - 1));

      java.util.concurrent.TimeUnit timeUnit =
          switch (unit) {
            case 'H' -> java.util.concurrent.TimeUnit.HOURS;
            case 'M' -> java.util.concurrent.TimeUnit.MINUTES;
            case 'S' -> java.util.concurrent.TimeUnit.SECONDS;
            case 'm' -> java.util.concurrent.TimeUnit.MILLISECONDS;
            case 'u' -> java.util.concurrent.TimeUnit.MICROSECONDS;
            case 'n' -> java.util.concurrent.TimeUnit.NANOSECONDS;
            default -> null;
          };

      if (timeUnit != null) {
        options = options.withDeadlineAfter(value, timeUnit);
      }
    } catch (Exception e) {
      log.debug("Failed to parse grpc-timeout header: {}", timeout);
    }

    return options;
  }

  /**
   * Maps standard HTTP headers from Jooby into gRPC Metadata. Skips HTTP/2 pseudo-headers and gRPC
   * internal headers.
   */
  private Metadata extractMetadata(Context ctx) {
    Metadata metadata = new Metadata();

    for (java.util.Map.Entry<String, String> header : ctx.headerMap().entrySet()) {
      String key = header.getKey().toLowerCase();

      // Ignore internal HTTP/2 and gRPC headers
      if (key.startsWith(":")
          || key.startsWith("grpc-")
          || key.equals("content-type")
          || key.equals("te")) {
        continue;
      }

      // If binary header (ends with -bin), gRPC requires base64 decoding.
      // Standard string headers are passed directly.
      if (key.endsWith("-bin")) {
        Metadata.Key<byte[]> metaKey = Metadata.Key.of(key, Metadata.BINARY_BYTE_MARSHALLER);
        byte[] decoded = java.util.Base64.getDecoder().decode(header.getValue());
        metadata.put(metaKey, decoded);
      } else {
        Metadata.Key<String> metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(metaKey, header.getValue());
      }
    }
    return metadata;
  }

  private StreamObserver<byte[]> wrapServerStreaming(
      ClientCall<byte[], byte[]> call, StreamObserver<byte[]> responseObserver) {
    // Server streaming takes 1 request and returns an observer for the result stream
    return new StreamObserver<>() {
      private boolean sent = false;

      @Override
      public void onNext(byte[] value) {
        if (!sent) {
          ClientCalls.asyncServerStreamingCall(call, value, responseObserver);
          sent = true;
        }
      }

      @Override
      public void onError(Throwable t) {
        responseObserver.onError(t);
      }

      @Override
      public void onCompleted() {
        /* Server side handles completion */
      }
    };
  }

  /**
   * Prepends the 5-byte gRPC header to the payload using a ByteBuffer.
   *
   * @param payload The raw binary message from the internal gRPC service.
   * @return A new byte array containing [Flag][Length][Payload].
   */
  private byte[] addGrpcHeader(byte[] payload) {
    ByteBuffer buffer = ByteBuffer.allocate(5 + payload.length);
    // 1. Compression Flag (0 = none)
    buffer.put((byte) 0);
    // 2. Encode Length as 4-byte Big Endian integer
    buffer.putInt(payload.length);
    // 3. Copy the actual payload
    buffer.put(payload);

    return buffer.array();
  }
}
