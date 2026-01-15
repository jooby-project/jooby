/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HexFormat;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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
import io.jooby.Sender;

public class UnifiedGrpcBridge implements Function<Context, Flow.Subscriber<byte[]>> {
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
  public Flow.Subscriber<byte[]> apply(Context context) {
    return new GrpcRequestBridge(context.getRequestPath(), startCall(context));
  }

  /**
   * Unified entry point to start an internal call. Handles Unary, Bidi, and Streaming via a single
   * StreamObserver interface.
   */
  public StreamObserver<byte[]> startCall(Context ctx) {
    // Setup gRPC response headers
    ctx.setResponseType("application/grpc");

    var path =
        ctx.getRequestPath(); // ctx.path("service").value() + "/" + ctx.path("method").value();
    var descriptor = methodRegistry.get(path.substring(1));
    if (descriptor == null) {
      terminateWithStatus(
          ctx,
          Status.UNIMPLEMENTED.withDescription("Method not found in bridge registry: " + path));
      return null;
    }

    var method =
        MethodDescriptor.<byte[], byte[]>newBuilder()
            .setType(descriptor.getType())
            .setFullMethodName(descriptor.getFullMethodName())
            .setRequestMarshaller(new RawMarshaller())
            .setResponseMarshaller(new RawMarshaller())
            .build();

    // 2. Prepare Call Options (Propagation of timeouts/metadata could happen here)
    CallOptions callOptions = CallOptions.DEFAULT;
    ClientCall<byte[], byte[]> call = channel.newCall(method, callOptions);

    ClientResponseObserver<byte[], byte[]> responseObserver;
    log.info("method type: {}", method.getType());
    var sender = ctx.responseSender(false);
    // Atomic guard to prevent multiple terminal calls
    var isFinished = new AtomicBoolean(false);
    sender.setTrailer("grpc-status", "0");
    // 3. Unified Response Observer (Handles data coming BACK from the server)
    responseObserver =
        new ClientResponseObserver<>() {
          @Override
          public void beforeStart(ClientCallStreamObserver<byte[]> requestStream) {
            requestStream.disableAutoInboundFlowControl();
          }

          @Override
          public void onNext(byte[] value) {
            if (isFinished.get()) return;
            log.info("onNext Send {}", HexFormat.of().formatHex(value));

            // Professional Framing: 5-byte header + payload

            byte[] framed = addGrpcHeader(value);
            sender.write(
                framed,
                new Sender.Callback() {
                  @Override
                  public void onComplete(@NonNull Context ctx, @Nullable Throwable cause) {
                    log.info("onNext Sent {}", ctx);
                    if (cause != null) {
                      onError(cause);
                    }
                  }
                });
          }

          @Override
          public void onError(Throwable t) {
            if (isFinished.compareAndSet(false, true)) {
              log.info(" error", t);
              terminateWithStatus(sender, Status.fromThrowable(t));
            }
          }

          @Override
          public void onCompleted() {
            if (isFinished.compareAndSet(false, true)) {
              log.info("onCompleted");
              terminateWithStatus(sender, Status.OK);
            }
          }
        };

    // 4. Map gRPC Method Type to the correct ClientCalls utility
    return switch (method.getType()) {
      case UNARY -> wrapUnary(call, responseObserver);
      case BIDI_STREAMING, CLIENT_STREAMING ->
          ClientCalls.asyncBidiStreamingCall(call, responseObserver);
      case SERVER_STREAMING -> wrapServerStreaming(call, responseObserver);
      default -> {
        terminateWithStatus(ctx, Status.INTERNAL.withDescription("Unsupported method type"));
        yield null;
      }
    };
  }

  private boolean isReflectionPath(String path) {
    return path.contains("ServerReflectionInfo");
  }

  private StreamObserver<byte[]> wrapUnary(
      ClientCall<byte[], byte[]> call, StreamObserver<byte[]> responseObserver) {
    // Unary expects a single message. We use the Bidi utility but logic ensures 1:1.
    return ClientCalls.asyncBidiStreamingCall(call, responseObserver);
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

  private void terminateWithStatus(Sender ctx, Status status) {
    ctx.setTrailer("grpc-status", String.valueOf(status.getCode().value()));
    if (status.getDescription() != null) {
      ctx.setTrailer("grpc-message", status.getDescription());
    }
    ctx.close();
  }

  /**
   * Professional Status Termination. Sets gRPC trailers and closes the Jetty response correctly.
   */
  private void terminateWithStatus(Context ctx, Status status) {
    ctx.setResponseTrailer("grpc-status", String.valueOf(status.getCode().value()));
    if (status.getDescription() != null) {
      ctx.setResponseTrailer("grpc-message", status.getDescription());
    }
    ctx.send("");
  }

  private byte[] addGrpcHeader(byte[] payload) {
    int len = payload.length;
    byte[] framed = new byte[5 + len];
    framed[0] = 0; // Uncompressed
    framed[1] = (byte) (len >> 24);
    framed[2] = (byte) (len >> 16);
    framed[3] = (byte) (len >> 8);
    framed[4] = (byte) len;
    System.arraycopy(payload, 0, framed, 5, len);
    return framed;
  }
}
