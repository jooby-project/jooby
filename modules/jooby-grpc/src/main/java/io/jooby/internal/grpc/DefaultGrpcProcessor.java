/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.grpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ClientResponseObserver;
import io.jooby.rpc.grpc.GrpcExchange;
import io.jooby.rpc.grpc.GrpcProcessor;

public class DefaultGrpcProcessor implements GrpcProcessor {

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
  private ManagedChannel channel;
  private final Map<String, MethodDescriptor<?, ?>> registry;

  public DefaultGrpcProcessor(Map<String, MethodDescriptor<?, ?>> registry) {
    this.registry = registry;
  }

  public void setChannel(ManagedChannel channel) {
    this.channel = channel;
  }

  @Override
  public boolean isGrpcMethod(String path) {
    // gRPC paths typically come in as "/package.Service/Method"
    // Our registry stores them as "package.Service/Method"
    String methodName = path.startsWith("/") ? path.substring(1) : path;

    // Quick O(1) hash map lookup
    return registry.get(methodName) != null;
  }

  @Override
  public Flow.Subscriber<ByteBuffer> process(GrpcExchange exchange) {
    // Route paths: /{package.Service}/{Method}
    String path = exchange.getRequestPath();
    // Remove the leading slash to match the gRPC method registry format
    var descriptor = registry.get(path.substring(1));

    if (descriptor == null) {
      // MUST never occur, it is guarded by {@link #isGrpcMethod}
      throw new IllegalStateException(
          "Unregistered gRPC method: '"
              + path
              + "'. "
              + "This request bypassed the GrpcProcessor.isGrpcMethod() guard, "
              + "indicating a bug or misconfiguration in the native server interceptor.");
    }

    var method =
        MethodDescriptor.<byte[], byte[]>newBuilder()
            .setType(descriptor.getType())
            .setFullMethodName(descriptor.getFullMethodName())
            .setRequestMarshaller(new RawMarshaller())
            .setResponseMarshaller(new RawMarshaller())
            .build();

    var callOptions = extractCallOptions(exchange);
    var metadata = extractMetadata(exchange);

    var interceptedChannel =
        io.grpc.ClientInterceptors.intercept(
            channel, io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(metadata));

    var call = interceptedChannel.newCall(method, callOptions);
    var isFinished = new AtomicBoolean(false);

    boolean isUnaryOrServerStreaming =
        method.getType() == MethodDescriptor.MethodType.UNARY
            || method.getType() == MethodDescriptor.MethodType.SERVER_STREAMING;

    var requestBridge = new GrpcRequestBridge(call, method.getType());

    var responseObserver =
        new ClientResponseObserver<byte[], byte[]>() {

          @Override
          public void beforeStart(ClientCallStreamObserver<byte[]> requestStream) {
            if (!isUnaryOrServerStreaming) {
              // requestStream.disableAutoInboundFlowControl();
              // Wire the readiness callback securely to the bridge
              requestStream.setOnReadyHandler(requestBridge::onGrpcReady);
              requestBridge.setRequestObserver(requestStream);
            }
          }

          @Override
          public void onNext(byte[] value) {
            if (isFinished.get()) return;

            ByteBuffer framed = addGrpcHeader(value);

            exchange.send(
                framed,
                cause -> {
                  if (cause != null) {
                    onError(cause);
                  }
                });
          }

          @Override
          public void onError(Throwable t) {
            if (isFinished.compareAndSet(false, true)) {
              log.debug("gRPC stream error", t);
              Status status = Status.fromThrowable(t);
              exchange.close(status.getCode().value(), status.getDescription());
            }
          }

          @Override
          public void onCompleted() {
            if (isFinished.compareAndSet(false, true)) {
              exchange.close(Status.OK.getCode().value(), null);
            }
          }
        };

    // 2. Inject the observer to break the circular dependency
    requestBridge.setResponseObserver(responseObserver);

    if (!isUnaryOrServerStreaming) {
      ClientCalls.asyncBidiStreamingCall(call, responseObserver);
    }

    return requestBridge;
  }

  /** Extracts the grpc-timeout header and applies it to CallOptions. */
  private CallOptions extractCallOptions(GrpcExchange exchange) {
    CallOptions options = CallOptions.DEFAULT;
    String timeout = exchange.getHeader("grpc-timeout");

    if (timeout == null || timeout.isEmpty()) {
      return options;
    }

    try {
      var unit = timeout.charAt(timeout.length() - 1);
      var value = Long.parseLong(timeout.substring(0, timeout.length() - 1));

      var timeUnit =
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

  /** Maps standard HTTP headers from the GrpcExchange into gRPC Metadata. */
  private io.grpc.Metadata extractMetadata(GrpcExchange exchange) {
    var metadata = new io.grpc.Metadata();

    for (var header : exchange.getHeaders().entrySet()) {
      var key = header.getKey().toLowerCase();

      if (key.startsWith(":")
          || key.startsWith("grpc-")
          || key.equals("content-type")
          || key.equals("te")) {
        continue;
      }

      if (key.endsWith("-bin")) {
        io.grpc.Metadata.Key<byte[]> metaKey =
            io.grpc.Metadata.Key.of(key, io.grpc.Metadata.BINARY_BYTE_MARSHALLER);
        byte[] decoded = java.util.Base64.getDecoder().decode(header.getValue());
        metadata.put(metaKey, decoded);
      } else {
        io.grpc.Metadata.Key<String> metaKey =
            io.grpc.Metadata.Key.of(key, io.grpc.Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(metaKey, header.getValue());
      }
    }
    return metadata;
  }

  /** Prepends the 5-byte gRPC header and returns a ready-to-write ByteBuffer. */
  private ByteBuffer addGrpcHeader(byte[] payload) {
    var buffer = ByteBuffer.allocate(5 + payload.length);
    buffer.put((byte) 0); // Compressed flag (0 = none)
    buffer.putInt(payload.length);
    buffer.put(payload);
    buffer.flip(); // Prepare the buffer for reading by the server socket
    return buffer;
  }
}
