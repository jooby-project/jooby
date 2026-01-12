/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.grpc.*;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Sender;
import io.jooby.exception.BadRequestException;

public class GrpcHandler implements Route.Handler {
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

  private final ManagedChannel channel;
  private final GrpcMethodRegistry methodRegistry;

  public GrpcHandler(ManagedChannel channel, GrpcMethodRegistry methodRegistry) {
    this.channel = channel;
    this.methodRegistry = methodRegistry;
  }

  @Override
  public Object apply(@NonNull Context ctx) throws Exception {
    // Detect gRPC content type
    String contentType = ctx.header("Content-Type").value();
    if (contentType == null || !contentType.contains("application/grpc")) {
      throw new BadRequestException(String.format("Content-Type: %s not supported", contentType));
    }

    // Setup gRPC response headers
    ctx.setResponseType(contentType);

    var path = ctx.path("service").value() + "/" + ctx.path("method").value();
    var descriptor = methodRegistry.get(path);
    if (descriptor == null) {
      return ctx.setResponseCode(404).send("Service not found");
    }
    var method =
        MethodDescriptor.<byte[], byte[]>newBuilder()
            .setType(descriptor.getType())
            .setFullMethodName(descriptor.getFullMethodName())
            .setRequestMarshaller(new RawMarshaller())
            .setResponseMarshaller(new RawMarshaller())
            .build();

    // 1. Initiate the internal gRPC call
    // We use byte[] marshallers to keep it raw and fast
    CompletableFuture<Object> future =
        switch (method.getType()) {
          case UNARY -> handleUnary(ctx, method);
          case BIDI_STREAMING -> handleBidi(ctx, method);
          //      case SERVER_STREAMING -> handleServerStreaming(ctx, method);
          //      case CLIENT_STREAMING -> handleClientStreaming(ctx, method);
          default ->
              CompletableFuture.failedFuture(new UnsupportedOperationException("Unknown type"));
        };
    return future;
  }

  private CompletableFuture<Object> handleBidi(Context ctx, MethodDescriptor<byte[], byte[]> method)
      throws IOException {
    CompletableFuture<Object> future = new CompletableFuture<>();

    var sender = ctx.responseSender(false);
    StreamObserver<byte[]> requestObserver =
        ClientCalls.asyncBidiStreamingCall(
            channel.newCall(method, CallOptions.DEFAULT),
            new StreamObserver<byte[]>() {
              @Override
              public void onNext(byte[] value) {
                ctx.setResponseTrailer("grpc-status", "0");
                sender.write(
                    addGrpcHeader(value),
                    new Sender.Callback() {
                      @Override
                      public void onComplete(@NonNull Context ctx, @Nullable Throwable cause) {}
                    });
              }

              @Override
              public void onError(Throwable t) {
                ctx.setResponseTrailer(
                    "grpc-status", Integer.toString(Status.fromThrowable(t).getCode().value()));
                future.completeExceptionally(t);
              }

              @Override
              public void onCompleted() {
                sender.close();
                future.complete(ctx);
              }
            });

    var is = ctx.body().stream();
    byte[] frame;
    while ((frame = readOneGrpcFrame(is)) != null) {
      requestObserver.onNext(frame);
    }
    requestObserver.onCompleted();
    return future;
  }

  private CompletableFuture<Object> handleUnary(
      Context ctx, MethodDescriptor<byte[], byte[]> method) throws IOException {
    CompletableFuture<Object> future = new CompletableFuture<>();
    byte[] requestPayload = readOneGrpcFrame(ctx.body().stream());
    ClientCalls.asyncUnaryCall(
        channel.newCall(method, CallOptions.DEFAULT),
        requestPayload,
        new StreamObserver<byte[]>() {
          @Override
          public void onNext(byte[] value) {
            ctx.setResponseTrailer("grpc-status", "0");
            ctx.send(addGrpcHeader(value));
          }

          @Override
          public void onError(Throwable t) {
            ctx.setResponseTrailer(
                "grpc-status", Integer.toString(Status.fromThrowable(t).getCode().value()));
            future.completeExceptionally(t);
          }

          @Override
          public void onCompleted() {
            future.complete(ctx);
          }
        });
    return future;
  }

  /**
   * Prepends the 5-byte gRPC header to the payload. * @param payload The raw binary message from
   * the internal gRPC service.
   *
   * @return A new byte array containing [Flag][Length][Payload].
   */
  private byte[] addGrpcHeader(byte[] payload) {
    int length = payload.length;
    byte[] framedMessage = new byte[5 + length];

    // 1. Compression Flag (0 = none)
    // We pass 0 because our bridge usually handles raw uncompressed bytes
    // or handles already-compressed payloads transparently.
    framedMessage[0] = 0;

    // 2. Encode Length as 4-byte Big Endian integer
    framedMessage[1] = (byte) ((length >> 24) & 0xFF);
    framedMessage[2] = (byte) ((length >> 16) & 0xFF);
    framedMessage[3] = (byte) ((length >> 8) & 0xFF);
    framedMessage[4] = (byte) (length & 0xFF);

    // 3. Copy the actual payload after the 5-byte header
    System.arraycopy(payload, 0, framedMessage, 5, length);

    return framedMessage;
  }

  /**
   * Reads exactly one gRPC frame from the input stream. * @param is The Jooby/Servlet input stream
   *
   * @return The raw protobuf payload (without the 5-byte header), or null if the end of the stream
   *     is reached.
   */
  private byte[] readOneGrpcFrame(InputStream is) throws IOException {
    // 1. Read the 5-byte gRPC header
    // Byte 0: Compression flag
    // Bytes 1-4: Message length (Big Endian)
    byte[] header = new byte[5];
    int bytesRead = is.readNBytes(header, 0, 5);

    if (bytesRead == 0) {
      return null; // Normal End of Stream (Half-close)
    }

    if (bytesRead < 5) {
      throw new IOException("Incomplete gRPC header. Expected 5 bytes, got " + bytesRead);
    }

    // 2. Extract the length (Big Endian)
    // We mask with 0xFF to treat bytes as unsigned
    int length =
        ((header[1] & 0xFF) << 24)
            | ((header[2] & 0xFF) << 16)
            | ((header[3] & 0xFF) << 8)
            | ((header[4] & 0xFF));

    if (length < 0) {
      throw new IOException("Invalid gRPC frame length: " + length);
    }

    // 3. Read exactly 'length' bytes for the payload
    byte[] payload = is.readNBytes(length);

    if (payload.length < length) {
      throw new IOException(
          "Incomplete gRPC payload. Expected " + length + " bytes, got " + payload.length);
    }

    return payload;
  }
}
