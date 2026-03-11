/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;

public class GrpcRequestBridge implements Subscriber<ByteBuffer> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ClientCallStreamObserver<byte[]> internalObserver;
  private final GrpcDeframer deframer;
  private Subscription subscription;
  private final AtomicBoolean completed = new AtomicBoolean(false);

  public GrpcRequestBridge(StreamObserver<byte[]> internalObserver) {
    this.deframer = new GrpcDeframer();
    this.internalObserver = (ClientCallStreamObserver<byte[]>) internalObserver;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    this.subscription = subscription;

    // Wire gRPC readiness to Jooby's Flow.Subscription
    internalObserver.setOnReadyHandler(
        () -> {
          if (internalObserver.isReady() && !completed.get()) {
            subscription.request(1);
          }
        });

    // Initial demand
    subscription.request(1);
  }

  @Override
  public void onNext(ByteBuffer item) {
    try {
      // Pass the zero-copy buffer straight to the deframer
      deframer.process(
          item,
          msg -> {
            internalObserver.onNext(msg);
          });

      // Only request more from the server if gRPC is ready
      if (internalObserver.isReady()) {
        subscription.request(1);
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
      internalObserver.onError(throwable);
    }
  }

  @Override
  public void onComplete() {
    if (completed.compareAndSet(false, true)) {
      internalObserver.onCompleted();
    }
  }
}
