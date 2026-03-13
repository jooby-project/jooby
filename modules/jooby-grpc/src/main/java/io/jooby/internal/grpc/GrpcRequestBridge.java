/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.grpc;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ClientResponseObserver;

public class GrpcRequestBridge implements Subscriber<ByteBuffer> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final GrpcDeframer deframer = new GrpcDeframer();
  private final AtomicBoolean completed = new AtomicBoolean(false);

  private final ClientCall<byte[], byte[]> call;
  private final MethodDescriptor.MethodType methodType;
  private final boolean isUnaryOrServerStreaming;

  private ClientResponseObserver<byte[], byte[]> responseObserver;
  private ClientCallStreamObserver<byte[]> requestObserver;
  private Subscription subscription;
  private byte[] singlePayload;

  public GrpcRequestBridge(
      ClientCall<byte[], byte[]> call, MethodDescriptor.MethodType methodType) {
    this.call = call;
    this.methodType = methodType;
    this.isUnaryOrServerStreaming =
        methodType == MethodDescriptor.MethodType.UNARY
            || methodType == MethodDescriptor.MethodType.SERVER_STREAMING;
  }

  public void setResponseObserver(ClientResponseObserver<byte[], byte[]> responseObserver) {
    this.responseObserver = responseObserver;
  }

  public void setRequestObserver(ClientCallStreamObserver<byte[]> requestObserver) {
    this.requestObserver = requestObserver;
  }

  public void onGrpcReady() {
    if (subscription != null
        && requestObserver != null
        && requestObserver.isReady()
        && !completed.get()) {
      subscription.request(1);
    }
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    this.subscription = subscription;
    // Initial demand to kick off the network body reader
    subscription.request(1);
  }

  @Override
  public void onNext(ByteBuffer item) {
    try {
      deframer.process(
          item,
          msg -> {
            if (isUnaryOrServerStreaming) {
              singlePayload = msg;
            } else {
              requestObserver.onNext(msg);
            }
          });

      if (isUnaryOrServerStreaming) {
        subscription.request(1); // Keep reading until EOF for unary/server-streaming
      } else if (requestObserver != null && requestObserver.isReady()) {
        subscription.request(1); // Ask for more if the streaming gRPC buffer is ready
      }
    } catch (Throwable t) {
      subscription.cancel();
      onError(t);
    }
  }

  @Override
  public void onError(Throwable throwable) {
    if (completed.compareAndSet(false, true)) {
      log.error("Error in gRPC request stream", throwable);
      if (requestObserver != null) {
        requestObserver.onError(throwable);
      } else if (responseObserver != null) {
        responseObserver.onError(throwable);
      }
    }
  }

  @Override
  public void onComplete() {
    if (completed.compareAndSet(false, true)) {
      if (isUnaryOrServerStreaming) {
        byte[] payload = singlePayload == null ? new byte[0] : singlePayload;
        if (methodType == MethodDescriptor.MethodType.UNARY) {
          ClientCalls.asyncUnaryCall(call, payload, responseObserver);
        } else {
          ClientCalls.asyncServerStreamingCall(call, payload, responseObserver);
        }
      } else if (requestObserver != null) {
        requestObserver.onCompleted();
      }
    }
  }
}
